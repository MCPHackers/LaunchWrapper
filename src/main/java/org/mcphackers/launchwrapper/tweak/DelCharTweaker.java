package org.mcphackers.launchwrapper.tweak;

import static org.mcphackers.launchwrapper.util.asm.InsnHelper.*;
import static org.objectweb.asm.Opcodes.*;

import org.mcphackers.launchwrapper.LaunchConfig;
import org.mcphackers.launchwrapper.util.ClassNodeSource;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/*
 * deAWT removes the usage of AWT Frame and Canvas from older versions of
 * Minecraft, fixing graphical issues like inverted colors on Apple silicon
 * hardware. When run with Minecraft 1.1 through 1.2.3 on macOS, the AWT removal
 * introduces a bug where the DEL control character (ASCII 127) is inserted as a
 * visible character in text fields.
 *
 * Normally, AWT handles platform-specific key mappings, but with AWT removed no
 * such handling exists. DEL insertion occurs specifically from Minecraft
 * version 1.1 through 1.2.3 because character validation logic was changed in
 * 1.1 in order to support 56 languages with extended ASCII characters. The
 * validation logic checks if a character is either in an allowed list or has an
 * ASCII value greater than 32. Because DEL is greater than 32, it's considered
 * a valid input and rendered as a replacement for the previous character.
 *
 * In Minecraft version 1.1, the validation logic appears in both GuiTextField
 * and GuiChat, typically obfuscated as aer and wm, respectively. In versions
 * 1.2.0 through 1.2.3, the logic was moved to ChatAllowedCharacters. In version
 * 1.2.4, GuiTextField and GuiChat were rewritten, fixing the bug.
 *
 * This tweaker targets Minecraft version 1.1 specifically. It modifies the
 * bytecode to reject ASCII values less than or equal to 127 instead of 32. This
 * excludes DEL while preserving intended support for extended ASCII. Regular
 * ASCII characters (Space, letters, numbers, etc.) remain unaffected as they're
 * included in the game's allowed list and thus pass the first part of the
 * validation condition.
 *
 * Additional links:
 *
 * - deAWT (https://github.com/kimoVoid/deAWT)
 * - RetroMCP-Java (https://github.com/MCPHackers/RetroMCP-Java)
 * - Prism Launcher (https://github.com/PrismLauncher/PrismLauncher)
 *
 */
public final class DelCharTweaker implements Tweaker {
	private final LaunchConfig config;

	public DelCharTweaker(final LaunchConfig config) {
		this.config = config;
	}

	public final boolean tweakClass(final ClassNodeSource source, final String name) {
		// Apply only when deAWT is enabled.
		if (!config.lwjglFrame.get()) {
			return false;
		}

		final ClassNode node = source.getClass(name);
		if (node == null) {
			return false;
		}

		for (final MethodNode method : node.methods) {
			AbstractInsnNode currentInstruction = getFirst(method.instructions);
			while (currentInstruction != null) {
				final IntInsnNode targetInstruction = getTargetInstruction(currentInstruction);
				if (targetInstruction == null) {
					currentInstruction = nextInsn(currentInstruction);
					continue;
				}

				// Change the ASCII value from Space (32) to DEL (127).
				targetInstruction.operand = 127;
				source.overrideClass(node);
				return true;
			}
		}

		return false;
	}

	/*
	 * Identifies the bytecode pattern and returns the BIPUSH instruction
	 * that needs to be modified when the pattern matches.
	 *
	 * To avoid reliance on any specific obfuscation, the tweaker simply looks
	 * for the unique bytecode pattern in any loaded class.
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
	private final IntInsnNode getTargetInstruction(final AbstractInsnNode ifge) {
		if (ifge.getOpcode() != IFGE) {
			return null;
		}

		final AbstractInsnNode iload = nextInsn(ifge);
		if (iload == null || iload.getOpcode() != ILOAD || ((VarInsnNode) iload).var != 1) {
			return null;
		}

		final AbstractInsnNode bipush = nextInsn(iload);
		if (bipush == null || bipush.getOpcode() != BIPUSH || ((IntInsnNode) bipush).operand != 32) {
			return null;
		}

		final AbstractInsnNode ifIcmple = nextInsn(bipush);
		if (ifIcmple == null || ifIcmple.getOpcode() != IF_ICMPLE) {
			return null;
		}

		return (IntInsnNode) bipush;
	}
}
