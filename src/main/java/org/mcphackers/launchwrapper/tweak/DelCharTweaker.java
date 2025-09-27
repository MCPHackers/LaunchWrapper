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
 * such handling exists. DEL insertion occurs when using deAWT with versions
 * 1.1 through 1.2.3 because character validation logic was changed in 1.1 to
 * support 56 languages with extended ASCII characters. The validation accepts a
 * character if it's either in an allowed list or has an ASCII value greater
 * than 32. Because DEL (127) is greater than 32, it's considered valid input
 * and rendered as a replacement character.
 *
 * In version 1.1, the validation logic appears in both GuiTextField and
 * GuiChat. In versions 1.2.0-1.2.3, it was moved to ChatAllowedCharacters.
 * In version 1.2.4, GuiTextField and GuiChat were rewritten, fixing the bug.
 *
 * This tweaker targets Minecraft 1.1 specifically. It modifies the bytecode to
 * reject ASCII values less than or equal to 127 instead of 32, excluding DEL
 * while preserving support for extended ASCII (128-255). Regular ASCII
 * characters (space, letters, numbers) remain unaffected as they're included
 * in the allowed list and pass the first validation condition.
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
		if (config == null || context == null) {
			throw new IllegalArgumentException(
					"Launch config and legacy tweak context must be non-null to construct DelCharTweaker.");
		}

		this.config = config;
		this.context = context;
	}

	/*
	 * Identifies the GuiScreen class name starting from the known Minecraft class.
	 *
	 * public void displayGuiScreen(GuiScreen screen) {
	 * // ...
	 * screen.setWorldAndResolution(this, int, int);
	 * // ...
	 * }
	 *
	 * First, finds methods matching the displayGuiScreen signature:
	 * public void (Object)
	 *
	 * Then examines each candidate for this bytecode sequence representing an
	 * invocation on the method parameter:
	 * ALOAD 1, ALOAD 0, ILOAD, ILOAD, INVOKEVIRTUAL
	 *
	 * Finally, verifies the invocation's owner type matches the method
	 * parameter and its signature matches setWorldAndResolution:
	 * public void (Minecraft, int, int)
	 *
	 * When all constraints are satisfied, the method parameter type is
	 * confirmed as GuiScreen and its name is returned.
	 */
	private String getGuiScreenName(ClassNode node) {
		for (MethodNode method : node.methods) {
			if ((method.access & ACC_PUBLIC) == 0) {
				continue;
			}

			Type[] argTypes = Type.getArgumentTypes(method.desc);
			if (argTypes.length != 1 || argTypes[0].getSort() != Type.OBJECT) {
				continue;
			}

			Type returnType = Type.getReturnType(method.desc);
			if (!returnType.equals(Type.VOID_TYPE)) {
				continue;
			}

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
						!invokeArgTypes[1].equals(Type.INT_TYPE) ||
						!invokeArgTypes[2].equals(Type.INT_TYPE)) {
					continue;
				}

				Type invokeReturnType = Type.getReturnType(invokeInsn.desc);
				if (!invokeReturnType.equals(Type.VOID_TYPE)) {
					continue;
				}

				return argTypes[0].getInternalName();
			}
		}

		return null;
	}

	/*
	 * Identifies GuiChat by its structure and inheritance.
	 *
	 * GuiChat extends GuiScreen and contains exactly three fields:
	 * - protected String message
	 * - private int updateCounter
	 * - private static final String allowedCharacters
	 *
	 * Further validated by checking for the keyTyped method:
	 * protected void keyTyped(char, int)
	 */
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

			if ((field.access & ACC_PROTECTED) != 0 &&
					(field.access & ACC_STATIC) == 0 &&
					fieldType.getSort() == Type.OBJECT &&
					fieldType.getClassName().equals("java.lang.String")) {
				hasProtectedString = true;
			} else if ((field.access & ACC_PRIVATE) != 0 &&
					(field.access & ACC_STATIC) == 0 &&
					fieldType.equals(Type.INT_TYPE)) {
				hasPrivateInt = true;
			} else if ((field.access & ACC_PRIVATE) != 0 &&
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

	/*
	 * Identifies GuiTextField by its constructor signature and inheritance.
	 *
	 * GuiTextField extends Gui and has a specific seven-parameter constructor:
	 * public GuiTextField(GuiScreen, FontRenderer, int, int, int, int, String)
	 */
	private boolean isGuiTextField(ClassNode node) {
		if (!node.superName.equals(guiName)) {
			return false;
		}

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
	 * Identifies and returns the BIPUSH instruction to modify.
	 *
	 * The validation logic in decompiled form:
	 * if (ChatAllowedCharacters.allowedCharacters.indexOf(var1) >= 0 || var1 > 32)
	 *
	 * The bytecode uses IF_ICMPLE with inverted logic, jumping to reject
	 * characters <= 32 and allowing those > 32. Changing the constant from 32
	 * to 127 rejects characters <= 127, excluding DEL.
	 *
	 * Bytecode pattern:
	 * - IFGE: Checks if indexOf result >= 0 (character in allowed list)
	 * - ILOAD_1: Loads the character parameter
	 * - BIPUSH 32: Pushes 32 (Space)
	 * - IF_ICMPLE: Compares and jumps if character <= 32
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
