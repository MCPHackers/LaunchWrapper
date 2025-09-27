package org.mcphackers.launchwrapper.tweak;

import static org.mcphackers.launchwrapper.util.asm.InsnHelper.compareInsn;
import static org.mcphackers.launchwrapper.util.asm.InsnHelper.fill;
import static org.mcphackers.launchwrapper.util.asm.InsnHelper.getFirst;
import static org.mcphackers.launchwrapper.util.asm.InsnHelper.nextInsn;
import static org.objectweb.asm.Opcodes.ACC_FINAL;
import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.objectweb.asm.Opcodes.ACC_PROTECTED;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_STATIC;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.BIPUSH;
import static org.objectweb.asm.Opcodes.IFGE;
import static org.objectweb.asm.Opcodes.IF_ICMPLE;
import static org.objectweb.asm.Opcodes.ILOAD;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;

import org.mcphackers.launchwrapper.LaunchConfig;
import org.mcphackers.launchwrapper.tweak.injection.legacy.LegacyTweakContext;
import org.mcphackers.launchwrapper.util.ClassNodeSource;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/*
 * deAWT removes the dependency on AWT Frame and Canvas from older versions of
 * Minecraft, fixing graphical issues like inverted colors on Apple silicon
 * hardware. When run with Minecraft 1.1 through 1.2.3 on macOS, the AWT removal
 * introduces a bug where the DEL control character (ASCII 127) is inserted as a
 * visible character in text fields.
 *
 * Normally, AWT handles platform-specific key mappings, but with AWT removed no
 * such handling exists. DEL insertion occurs specifically from Minecraft
 * version 1.1 through 1.2.3 because character validation logic was changed in
 * 1.1 in order to support 56 languages with extended ASCII characters. The
 * validation logic accepts a character if it's either in an allowed list or has
 * an ASCII value greater than 32. Because DEL is greater than 32, it's
 * considered a valid input and rendered as a replacement for the previous
 * character.
 *
 * In Minecraft version 1.1, the validation logic appears in both GuiTextField
 * and GuiChat. In versions 1.2.0 through 1.2.3, the logic was moved to
 * ChatAllowedCharacters. In version 1.2.4, GuiTextField and GuiChat were
 * rewritten, fixing the bug.
 *
 * This tweaker targets Minecraft version 1.1 specifically. It modifies the
 * bytecode to reject ASCII values less than or equal to 127 instead of 32. This
 * excludes DEL while preserving intended support for extended ASCII (128-255).
 * Regular ASCII characters (Space, letters, numbers, etc.) remain unaffected as
 * they're included in the game's allowed list and thus pass the first part of
 * the validation condition.
 *
 * Additional links:
 *
 * - deAWT (https://github.com/kimoVoid/deAWT)
 * - RetroMCP-Java (https://github.com/MCPHackers/RetroMCP-Java)
 * - Prism Launcher (https://github.com/PrismLauncher/PrismLauncher)
 *
 */
public class DelCharTweaker implements Tweaker {
	private LegacyTweakContext context;
	private LaunchConfig config;

	private String guiName;
	private String guiScreenName;

	public DelCharTweaker(LaunchConfig config, LegacyTweakContext context) {
		this.config = config;
		this.context = context;
	}

	private String getGuiScreenName(ClassNode node) {
		// public void displayGuiScreen(GuiScreen)
		for (MethodNode method : node.methods) {
			if ((method.access & ACC_PUBLIC) == 0) {
				continue;
			}

			Type[] argTypes = Type.getArgumentTypes(method.desc);
			if (argTypes.length != 1 || argTypes[0].getSort() != Type.OBJECT) {
				continue;
			}

			Type returnType = Type.getReturnType(method.desc);
			if (returnType.getSort() != Type.VOID) {
				continue;
			}

			// ((GuiScreen) _).setWorldAndResolution(Minecraft, int, int)
			// ALOAD 1, ALOAD 0, ILOAD, ILOAD, INVOKEVIRTUAL
			for (int index = 0; index <= method.instructions.size() - 5; index++) {
				AbstractInsnNode[] insns = fill(method.instructions.get(index), 5);

				if (!compareInsn(insns[0], ALOAD, 1) || !compareInsn(insns[1], ALOAD, 0) ||
						!compareInsn(insns[2], ILOAD) || !compareInsn(insns[3], ILOAD) ||
						!compareInsn(insns[4], INVOKEVIRTUAL)) {
					continue;
				}

				MethodInsnNode invokeInsn = (MethodInsnNode) insns[4];
				if (!invokeInsn.owner.equals(argTypes[0].getInternalName())) {
					continue;
				}

				Type[] invokeArgTypes = Type.getArgumentTypes(invokeInsn.desc);
				if (invokeArgTypes.length != 3 ||
						invokeArgTypes[0].getSort() != Type.OBJECT ||
						!invokeArgTypes[0].getInternalName().equals(node.name) ||
						invokeArgTypes[1].getSort() != Type.INT ||
						invokeArgTypes[2].getSort() != Type.INT) {
					continue;
				}

				Type invokeReturnType = Type.getReturnType(invokeInsn.desc);
				if (invokeReturnType.getSort() != Type.VOID) {
					continue;
				}

				return argTypes[0].getInternalName();
			}
		}

		return null;
	}

	private boolean isGuiChat(ClassNode node) {
		if (!node.superName.equals(guiScreenName)) {
			return false;
		}

		if (node.fields.size() != 3) {
			return false;
		}

		boolean hasProtectedString = false;
		boolean hasPrivateInt = false;
		boolean hasPrivateStaticFinalString = false;

		for (FieldNode field : node.fields) {
			Type fieldType = Type.getType(field.desc);

			// protected String message
			if ((field.access & ACC_PROTECTED) != 0 &&
					(field.access & ACC_STATIC) == 0 &&
					fieldType.getSort() == Type.OBJECT &&
					fieldType.getClassName().equals("java.lang.String")) {
				hasProtectedString = true;
			}
			// private int updateCounter
			else if ((field.access & ACC_PRIVATE) != 0 &&
					(field.access & ACC_STATIC) == 0 &&
					fieldType.equals(Type.INT_TYPE)) {
				hasPrivateInt = true;
			}
			// private static final String allowedCharacters
			else if ((field.access & ACC_PRIVATE) != 0 &&
					(field.access & ACC_STATIC) != 0 &&
					(field.access & ACC_FINAL) != 0 &&
					fieldType.getSort() == Type.OBJECT &&
					fieldType.getClassName().equals("java.lang.String")) {
				hasPrivateStaticFinalString = true;
			}
		}

		if (!hasProtectedString || !hasPrivateInt || !hasPrivateStaticFinalString) {
			return false;
		}

		// protected keyTyped (char, int) -> void
		for (MethodNode method : node.methods) {
			if ((method.access & ACC_PROTECTED) == 0) {
				continue;
			}

			Type[] argTypes = Type.getArgumentTypes(method.desc);
			if (argTypes.length != 2 || !argTypes[0].equals(Type.CHAR_TYPE) ||
					!argTypes[1].equals(Type.INT_TYPE)) {
				continue;
			}

			Type returnType = Type.getReturnType(method.desc);
			if (returnType.equals(Type.VOID_TYPE)) {
				return true;
			}
		}

		return false;
	}

	private boolean isGuiTextField(ClassNode node) {
		if (!node.superName.equals(guiName)) {
			return false;
		}

		// public GuiTextField(GuiScreen, FontRenderer, int, int, int, int, String)
		for (MethodNode method : node.methods) {
			if (!method.name.equals("<init>") || (method.access & ACC_PUBLIC) == 0) {
				continue;
			}

			Type[] argTypes = Type.getArgumentTypes(method.desc);

			if (argTypes.length == 7 &&
					argTypes[0].getSort() == Type.OBJECT &&
					argTypes[0].getInternalName().equals(guiScreenName) &&
					argTypes[1].getSort() == Type.OBJECT && // FontRenderer
					argTypes[2].equals(Type.INT_TYPE) &&
					argTypes[3].equals(Type.INT_TYPE) &&
					argTypes[4].equals(Type.INT_TYPE) &&
					argTypes[5].equals(Type.INT_TYPE) &&
					argTypes[6].getSort() == Type.OBJECT
					&& argTypes[6].getClassName().equals("java.lang.String")) {
				return true;
			}
		}

		return false;
	}

	/*
	 * Identifies the bytecode pattern and returns the BIPUSH instruction
	 * that needs to be modified when the pattern matches.
	 *
	 * The validation logic in decompiled form:
	 * if (ChatAllowedCharacters.allowedCharacters.indexOf(var1) >= 0 || var1 > 32)
	 *
	 * The bytecode uses IF_ICMPLE (if less than or equal) with inverted
	 * logic. It jumps to reject the character if it's <= 32, allowing
	 * it through if > 32. By changing the constant from 32 to 127,
	 * characters <= 127 are rejected, which excludes DEL.
	 *
	 * The bytecode pattern:
	 * IFGE [2 offset bytes], ILOAD_1, BIPUSH 32, IF_ICMPLE [2 offset bytes]
	 *
	 * Pattern sequence:
	 * - IFGE: Checks if the indexOf result is >= 0 (character is in allowed list)
	 * - ILOAD_1: Loads the character parameter onto the stack
	 * - BIPUSH 32: Pushes the ASCII value for Space onto the stack
	 * - IF_ICMPLE: Compares and jumps if the character is <= 32
	 */
	private IntInsnNode getTargetInsn(AbstractInsnNode insn) {
		AbstractInsnNode[] insns = fill(insn, 4);

		if (!compareInsn(insns[0], IFGE) ||
				!compareInsn(insns[1], ILOAD, 1) ||
				!compareInsn(insns[2], BIPUSH, 32) ||
				!compareInsn(insns[3], IF_ICMPLE)) {
			return null;
		}

		return (IntInsnNode) insns[2];
	}

	public boolean tweakClass(ClassNodeSource source, String name) {
		if (!config.lwjglFrame.get()) {
			return false;
		}

		ClassNode minecraftNode = this.context.getMinecraft();
		if (minecraftNode == null) {
			return false;
		}

		if (guiScreenName == null) {
			String guiScreenName = getGuiScreenName(minecraftNode);
			if (guiScreenName == null) {
				return false;
			}

			this.guiScreenName = guiScreenName;
		}

		ClassNode guiScreenNode = source.getClass(guiScreenName);
		if (guiScreenNode == null) {
			return false;
		}

		if (guiName == null) {
			String guiName = guiScreenNode.superName;
			if (guiName == null) {
				return false;
			}

			this.guiName = guiName;
		}

		ClassNode sourceNode = source.getClass(name);
		if (sourceNode == null) {
			return false;
		}

		if (!isGuiTextField(sourceNode) && !isGuiChat(sourceNode)) {
			return false;
		}

		for (MethodNode method : sourceNode.methods) {
			for (AbstractInsnNode currentInsn = getFirst(
					method.instructions); currentInsn != null; currentInsn = nextInsn(currentInsn)) {
				IntInsnNode targetInsn = getTargetInsn(currentInsn);
				if (targetInsn == null) {
					continue;
				}

				// Change the BIPUSH operand from 32 (Space) to 127 (DEL).
				targetInsn.operand = 127;
				source.overrideClass(sourceNode);
				return true;
			}
		}

		return false;
	}
}
