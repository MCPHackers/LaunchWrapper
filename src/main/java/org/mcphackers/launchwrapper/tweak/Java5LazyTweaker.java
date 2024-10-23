package org.mcphackers.launchwrapper.tweak;

import static org.mcphackers.launchwrapper.util.asm.InsnHelper.*;
import static org.objectweb.asm.Opcodes.*;

import org.mcphackers.launchwrapper.loader.LaunchClassLoader;
import org.mcphackers.launchwrapper.util.ClassNodeSource;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Compatibility fixes for Java 5.
 * Replaces most of Java 6 API used by Minecraft with Java 5 alternative
 */
public class Java5LazyTweaker implements Tweaker {

	public boolean tweakClass(ClassNodeSource source, String name) {
		ClassNode node = source.getClass(name);
		if (node == null) {
			return false;
		}
		boolean changed = false;
		// Decrease class version
		if (node.version > LaunchClassLoader.CLASS_VERSION) {
			node.version = LaunchClassLoader.CLASS_VERSION;
			changed = true;
		}
		for (MethodNode m : node.methods) {
			for (AbstractInsnNode insn : iterator(m.instructions)) {
				AbstractInsnNode[] insns = fill(insn, 3);
				// Replace String.isEmpty() with String.length == 0
				if (compareInsn(insn, INVOKEVIRTUAL, "java/lang/String", "isEmpty", "()Z")) {
					MethodInsnNode invoke = new MethodInsnNode(INVOKEVIRTUAL, "java/lang/String", "length", "()I");
					m.instructions.set(insns[0], invoke);
					InsnList insert = new InsnList();
					LabelNode not = new LabelNode();
					LabelNode end = new LabelNode();
					insert.add(new JumpInsnNode(IFEQ, not));
					insert.add(booleanInsn(false));
					insert.add(new JumpInsnNode(GOTO, end));
					insert.add(not);
					insert.add(booleanInsn(true));
					insert.add(end);
					m.instructions.insert(invoke, insert);
					insn = nextInsn(invoke);
					changed = true;
					continue;
				}
				// Replace String.getBytes(Charset.forName(charset)) with String.getBytes(charset)
				if (compareInsn(insns[1], INVOKESTATIC, "java/nio/charset/Charset", "forName", "(Ljava/lang/String;)Ljava/nio/charset/Charset;") &&
					compareInsn(insns[2], INVOKEVIRTUAL, "java/lang/String", "getBytes", "(Ljava/nio/charset/Charset;)[B") &&
					compareInsn(insns[0], LDC)) {
					m.instructions.set(insns[2], new MethodInsnNode(INVOKEVIRTUAL, "java/lang/String", "getBytes", "(Ljava/lang/String;)[B"));
					m.instructions.remove(insns[1]);
					changed = true;
					continue;
				}
				// Same thing but for new String()
				if (compareInsn(insns[1], INVOKESTATIC, "java/nio/charset/Charset", "forName", "(Ljava/lang/String;)Ljava/nio/charset/Charset;") &&
					compareInsn(insns[2], INVOKESPECIAL, "java/lang/String", "<init>", "([BLjava/nio/charset/Charset;)V") &&
					compareInsn(insns[0], LDC)) {
					m.instructions.set(insns[2], new MethodInsnNode(INVOKESPECIAL, "java/lang/String", "<init>", "([BLjava/lang/String;)V"));
					m.instructions.remove(insns[1]);
					changed = true;
					continue;
				}
			}
		}
		if (changed) {
			source.overrideClass(node);
		}
		return changed;
	}
}
