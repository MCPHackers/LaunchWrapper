package org.mcphackers.launchwrapper.tweak.injection.legacy;

import static org.mcphackers.launchwrapper.util.asm.InsnHelper.*;
import static org.objectweb.asm.Opcodes.*;

import org.mcphackers.launchwrapper.LaunchConfig;
import org.mcphackers.launchwrapper.tweak.injection.InjectionWithContext;
import org.mcphackers.launchwrapper.tweak.injection.MinecraftGetter;
import org.mcphackers.launchwrapper.util.ClassNodeSource;
import org.mcphackers.launchwrapper.util.asm.NodeHelper;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * Level generator updates screen each step, which locks main thread with VSync enabled.
 * c0.30 fixed it by returning early if the call happens too quickly
 */
public class ClassicLoadingFix extends InjectionWithContext<MinecraftGetter> {

	public ClassicLoadingFix(MinecraftGetter context) {
		super(context);
	}

	public String name() {
		return "Classic loading fix";
	}

	public boolean required() {
		return false;
	}

	protected ClassNode getLoadingScreen(ClassNodeSource source) {
		ClassNode minecraft = context.getMinecraft();
		for (MethodNode m : NodeHelper.getConstructors(minecraft)) {
			for (AbstractInsnNode insn = getFirst(m.instructions); insn != null; insn = nextInsn(insn)) {
				AbstractInsnNode[] insns = fill(insn, 4);
				if (compareInsn(insns[0], NEW) &&
					compareInsn(insns[1], DUP) &&
					compareInsn(insns[2], ALOAD, 0) && insns[3] != null && insns[3].getType() == AbstractInsnNode.METHOD_INSN &&
					compareInsn(insns[3], -1, null, "<init>", "(L" + minecraft.name + ";)V")) {
					ClassNode test = source.getClass(((TypeInsnNode)insn).desc);
					if (test == null) {
						continue;
					}
					if (hasLoading(test)) {
						return test;
					}
				}
			}
		}
		if (hasLoading(minecraft)) {
			return minecraft;
		}
		return null;
	}

	public FieldNode newField(ClassNode node, int access, String name, String desc) {
		while (true) {
			boolean conflict = false;
			for (FieldNode f : node.fields) {
				if (f.name.equals(name)) {
					conflict = true;
					name += "_";
					break;
				}
			}
			if (!conflict) {
				break;
			}
		}
		FieldNode newField = new FieldNode(access, name, desc, null, null);
		node.fields.add(newField);
		return newField;
	}

	private boolean hasLoading(ClassNode node) {
		for (MethodNode m2 : NodeHelper.getConstructors(node)) {
			int emptyStrings = 0;
			for (AbstractInsnNode insn2 = getFirst(m2.instructions); insn2 != null; insn2 = nextInsn(insn2)) {
				if (compareInsn(insn2, LDC, "") &&
					compareInsn(nextInsn(insn2), PUTFIELD, node.name, null, "Ljava/lang/String;")) {
					emptyStrings++;
				}
				if (emptyStrings == 2) {
					return true;
				}
			}
		}
		return false;
	}

	protected MethodNode getUpdateProgress(ClassNode node) {
		for (MethodNode m : node.methods) {
			if (!m.desc.equals("(I)V")) {
				continue;
			}
			for (AbstractInsnNode insn = getFirst(m.instructions); insn != null; insn = nextInsn(insn)) {
				if (compareInsn(insn, LDC, "/dirt.png")) {
					return m;
				}
			}
		}
		return null;
	}

	public boolean apply(ClassNodeSource source, LaunchConfig config) {
		ClassNode loadingScreen = getLoadingScreen(source);
		if (loadingScreen == null) {
			return false;
		}
		MethodNode updateProgress = getUpdateProgress(loadingScreen);
		if (updateProgress == null) {
			return false;
		}
		for (AbstractInsnNode insn = getFirst(updateProgress.instructions); insn != null; insn = nextInsn(insn)) {
			if (compareInsn(insn, INVOKESTATIC, "java/lang/System", "currentTimeMillis", "()J")) {
				// It's already patched
				return false;
			}
		}
		FieldNode f = newField(loadingScreen, ACC_PRIVATE, "lastUpdate", "J");
		for (MethodNode m : NodeHelper.getConstructors(loadingScreen)) {
			InsnList inject = new InsnList();
			inject.add(new VarInsnNode(ALOAD, 0));
			inject.add(new MethodInsnNode(INVOKESTATIC, "java/lang/System", "currentTimeMillis", "()J"));
			inject.add(new FieldInsnNode(PUTFIELD, loadingScreen.name, f.name, f.desc));
			m.instructions.insert(inject);
		}
		int var = getFreeIndex(updateProgress.instructions);

		// // Code from c0.30
		// long var = System.currentTimeMillis();
		// if (var - this.lastUpdate >= 0L && var - this.lastUpdate < 20L) {
		//     return;
		// }
		// this.lastUpdate = var;

		InsnList inject = new InsnList();
		LabelNode l = new LabelNode();
		inject.add(new MethodInsnNode(INVOKESTATIC, "java/lang/System", "currentTimeMillis", "()J"));
		inject.add(new InsnNode(DUP2));
		inject.add(new VarInsnNode(LSTORE, var));
		inject.add(new VarInsnNode(ALOAD, 0));
		inject.add(new FieldInsnNode(GETFIELD, loadingScreen.name, f.name, f.desc));
		inject.add(new InsnNode(LSUB));
		inject.add(new InsnNode(LCONST_0));
		inject.add(new InsnNode(LCMP));
		inject.add(new JumpInsnNode(IFLT, l));
		inject.add(new VarInsnNode(LLOAD, var));
		inject.add(new VarInsnNode(ALOAD, 0));
		inject.add(new FieldInsnNode(GETFIELD, loadingScreen.name, f.name, f.desc));
		inject.add(new InsnNode(LSUB));
		inject.add(longInsn(20L));
		inject.add(new InsnNode(LCMP));
		inject.add(new JumpInsnNode(IFGE, l));
		inject.add(new InsnNode(RETURN));
		inject.add(l);
		inject.add(new VarInsnNode(ALOAD, 0));
		inject.add(new VarInsnNode(LLOAD, var));
		inject.add(new FieldInsnNode(PUTFIELD, loadingScreen.name, f.name, f.desc));
		updateProgress.instructions.insert(inject);
		source.overrideClass(loadingScreen);
		return true;
	}
}
