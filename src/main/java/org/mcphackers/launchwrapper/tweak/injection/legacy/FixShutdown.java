package org.mcphackers.launchwrapper.tweak.injection.legacy;

import static org.mcphackers.rdi.util.InsnHelper.*;
import static org.objectweb.asm.Opcodes.*;

import org.mcphackers.launchwrapper.LaunchConfig;
import org.mcphackers.launchwrapper.tweak.injection.InjectionWithContext;
import org.mcphackers.launchwrapper.util.ClassNodeSource;
import org.mcphackers.rdi.util.NodeHelper;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.VarInsnNode;

public class FixShutdown extends InjectionWithContext<LegacyTweakContext> {

	public FixShutdown(LegacyTweakContext storage) {
        super(storage);
    }

    @Override
    public String name() {
        return "Fix Shutdown";
    }

    @Override
	public boolean required() {
		return false;
	}

    @Override
    public boolean apply(ClassNodeSource source, LaunchConfig config) {
        fixShutdown(source);
        return true;
    }

    private void fixShutdown(ClassNodeSource source) {
		MethodNode destroy = null;
		if(context.run != null) {
			AbstractInsnNode insn1 = context.run.instructions.getLast();
			while(insn1 != null && insn1.getOpcode() != ATHROW) {
				insn1 = previousInsn(insn1);
			}
			AbstractInsnNode[] insns1 = fillBackwards(insn1, 4);
			if(compareInsn(insns1[3], ATHROW)
			&& compareInsn(insns1[1], INVOKEVIRTUAL, context.minecraft.name, null, "()V")
			&& compareInsn(insns1[0], ALOAD)
			&& compareInsn(insns1[2], ALOAD)) {
				MethodInsnNode invoke = (MethodInsnNode) insns1[1];
				destroy = NodeHelper.getMethod(context.minecraft, invoke.name, invoke.desc);
			}
		}
		if(destroy == null) {
			for(MethodNode m : context.minecraft.methods) {
				if(containsInvoke(m.instructions, new MethodInsnNode(INVOKESTATIC, "org/lwjgl/input/Mouse", "destroy", "()V"))
				&& containsInvoke(m.instructions, new MethodInsnNode(INVOKESTATIC, "org/lwjgl/input/Keyboard", "destroy", "()V"))
				&& containsInvoke(m.instructions, new MethodInsnNode(INVOKESTATIC, "org/lwjgl/opengl/Display", "destroy", "()V"))) {
					destroy = m;
					// tweakInfo("destroy()", destroy.name + destroy.desc);
					break;
				}
			}
		}
		AbstractInsnNode insn1 = context.run.instructions.getFirst();
		if(destroy != null) {
			while(insn1 != null) {
				if(insn1.getOpcode() == RETURN &&
				!compareInsn(insn1.getPrevious(), INVOKEVIRTUAL, context.minecraft.name, destroy.name, destroy.desc)) {
					InsnList insert = new InsnList();
					insert.add(new VarInsnNode(ALOAD, 0));
					insert.add(new MethodInsnNode(INVOKEVIRTUAL, context.minecraft.name, destroy.name, destroy.desc));
					context.run.instructions.insertBefore(insn1, insert);
				}
				insn1 = nextInsn(insn1);
			}
			insn1 = destroy.instructions.getFirst();
			while(insn1 != null) {
				if(compareInsn(insn1, INVOKESTATIC, "org/lwjgl/opengl/Display", "destroy", "()V")) {
					AbstractInsnNode insn3 = nextInsn(insn1);
					if(insn3 != null) {
						AbstractInsnNode[] insns2 = fill(insn3, 2);
						if(compareInsn(insns2[0], ICONST_0)
						&& compareInsn(insns2[1], INVOKESTATIC, "java/lang/System", "exit", "(I)V")) {
						} else {
							InsnList insert = new InsnList();
							insert.add(new InsnNode(ICONST_0));
							insert.add(new MethodInsnNode(INVOKESTATIC, "java/lang/System", "exit", "(I)V"));
							destroy.instructions.insert(insn1, insert);
							// tweakInfo("Shutdown patch");
						}
					}
				}
				insn1 = nextInsn(insn1);
			}

			boolean setWorldIsWrapped = false;
			for(TryCatchBlockNode tryCatch : destroy.tryCatchBlocks) {
				AbstractInsnNode insn = nextInsn(tryCatch.start);
				AbstractInsnNode[] insns2 = fill(insn, 3);
				if(compareInsn(insns2[0], ALOAD)
				&& compareInsn(insns2[1], ACONST_NULL)
				&& compareInsn(insns2[2], INVOKEVIRTUAL, context.minecraft.name, null, null)) {
					MethodInsnNode invoke = (MethodInsnNode) insns2[2];
					if(Type.getReturnType(invoke.desc).getSort() == Type.VOID) {
						setWorldIsWrapped = true;
						break;
					}
				}
			}
			if(!setWorldIsWrapped) {
				insn1 = destroy.instructions.getFirst();
				while(insn1 != null) {
					AbstractInsnNode[] insns2 = fill(insn1, 3);
					if(compareInsn(insns2[0], ALOAD)
					&& compareInsn(insns2[1], ACONST_NULL)
					&& compareInsn(insns2[2], INVOKEVIRTUAL, context.minecraft.name, null, null)) {
						MethodInsnNode invoke = (MethodInsnNode) insns2[2];
						if(Type.getReturnType(invoke.desc).getSort() == Type.VOID) {
							addTryCatch(destroy, insns2[0], insns2[2], "java/lang/Throwable");
							// tweakInfo("SoundManager shutdown");
							break;
						}
					}
					insn1 = nextInsn(insn1);
				}
			}
		}
	}
    
}
