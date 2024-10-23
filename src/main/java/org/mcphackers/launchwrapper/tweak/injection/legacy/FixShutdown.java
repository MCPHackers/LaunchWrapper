package org.mcphackers.launchwrapper.tweak.injection.legacy;

import static org.mcphackers.launchwrapper.util.asm.InsnHelper.*;
import static org.objectweb.asm.Opcodes.*;

import org.mcphackers.launchwrapper.LaunchConfig;
import org.mcphackers.launchwrapper.tweak.injection.InjectionWithContext;
import org.mcphackers.launchwrapper.tweak.injection.MinecraftGetter;
import org.mcphackers.launchwrapper.util.ClassNodeSource;
import org.mcphackers.launchwrapper.util.asm.NodeHelper;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * This patch makes the game exit *cleanly* by properly running deinitialization method
 * This also includes fixes to certain soft locks when the game crashes
 */
public class FixShutdown extends InjectionWithContext<MinecraftGetter> {

	public FixShutdown(MinecraftGetter storage) {
		super(storage);
	}

	public String name() {
		return "Fix Shutdown";
	}

	public boolean required() {
		return false;
	}

	public boolean apply(ClassNodeSource source, LaunchConfig config) {
		ClassNode minecraft = context.getMinecraft();
		MethodNode run = context.getRun();
		MethodNode destroy = null;
		if (run == null) {
			return false;
		}

		boolean b = false;
		AbstractInsnNode insn1 = run.instructions.getLast();
		while (insn1 != null && insn1.getOpcode() != ATHROW) {
			insn1 = previousInsn(insn1);
		}
		AbstractInsnNode[] insns1 = fillBackwards(insn1, 4);
		if (compareInsn(insns1[3], ATHROW) &&
			compareInsn(insns1[1], INVOKEVIRTUAL, minecraft.name, null, "()V") &&
			compareInsn(insns1[0], ALOAD) &&
			compareInsn(insns1[2], ALOAD)) {
			MethodInsnNode invoke = (MethodInsnNode)insns1[1];
			destroy = NodeHelper.getMethod(minecraft, invoke.name, invoke.desc);
		}
		if (destroy == null) {
			for (MethodNode m : minecraft.methods) {
				if (containsInvoke(m.instructions, new MethodInsnNode(INVOKESTATIC, "org/lwjgl/input/Mouse", "destroy", "()V")) && containsInvoke(m.instructions, new MethodInsnNode(INVOKESTATIC, "org/lwjgl/input/Keyboard", "destroy", "()V")) && containsInvoke(m.instructions, new MethodInsnNode(INVOKESTATIC, "org/lwjgl/opengl/Display", "destroy", "()V"))) {
					destroy = m;
					break;
				}
			}
		}
		insn1 = getFirst(run.instructions);
		if (destroy != null) {
			while (insn1 != null) {
				if (insn1.getOpcode() == RETURN &&
					!compareInsn(insn1.getPrevious(), INVOKEVIRTUAL, minecraft.name, destroy.name, destroy.desc)) {
					InsnList insert = new InsnList();
					insert.add(new VarInsnNode(ALOAD, 0));
					insert.add(new MethodInsnNode(INVOKEVIRTUAL, minecraft.name, destroy.name, destroy.desc));
					run.instructions.insertBefore(insn1, insert);
				}
				insn1 = nextInsn(insn1);
			}
			insn1 = getFirst(destroy.instructions);
			while (insn1 != null) {
				if (compareInsn(insn1, INVOKESTATIC, "org/lwjgl/opengl/Display", "destroy", "()V")) {
					AbstractInsnNode insn3 = nextInsn(insn1);
					if (insn3 != null) {
						AbstractInsnNode[] insns2 = fill(insn3, 2);
						if (compareInsn(insns2[0], ICONST_0) &&
							compareInsn(insns2[1], INVOKESTATIC, "java/lang/System", "exit", "(I)V")) {
						} else {
							InsnList insert = new InsnList();
							insert.add(new InsnNode(ICONST_0));
							insert.add(new MethodInsnNode(INVOKESTATIC, "java/lang/System", "exit", "(I)V"));
							destroy.instructions.insert(insn1, insert);
							b = true;
						}
					}
				}
				insn1 = nextInsn(insn1);
			}

			boolean setWorldIsWrapped = false;
			for (TryCatchBlockNode tryCatch : destroy.tryCatchBlocks) {
				AbstractInsnNode insn = nextInsn(tryCatch.start);
				AbstractInsnNode[] insns2 = fill(insn, 3);
				if (compareInsn(insns2[0], ALOAD) &&
					compareInsn(insns2[1], ACONST_NULL) &&
					compareInsn(insns2[2], INVOKEVIRTUAL, minecraft.name, null, null)) {
					MethodInsnNode invoke = (MethodInsnNode)insns2[2];
					if (Type.getReturnType(invoke.desc).getSort() == Type.VOID) {
						setWorldIsWrapped = true;
						break;
					}
				}
			}
			if (!setWorldIsWrapped) {
				insn1 = getFirst(destroy.instructions);
				while (insn1 != null) {
					AbstractInsnNode[] insns2 = fill(insn1, 3);
					if (compareInsn(insns2[0], ALOAD) &&
						compareInsn(insns2[1], ACONST_NULL) &&
						compareInsn(insns2[2], INVOKEVIRTUAL, minecraft.name, null, null)) {
						MethodInsnNode invoke = (MethodInsnNode)insns2[2];
						if (Type.getReturnType(invoke.desc).getSort() == Type.VOID) {
							addTryCatch(destroy, insns2[0], insns2[2], "java/lang/Throwable");
							b = true;
							break;
						}
					}
					insn1 = nextInsn(insn1);
				}
			}
		}
		return b;
	}
}
