package org.mcphackers.launchwrapper.tweak.injection.legacy;

import static org.mcphackers.launchwrapper.util.InsnHelper.*;
import static org.mcphackers.rdi.util.InsnHelper.*;
import static org.objectweb.asm.Opcodes.*;

import org.mcphackers.launchwrapper.LaunchConfig;
import org.mcphackers.launchwrapper.tweak.injection.InjectionWithContext;
import org.mcphackers.launchwrapper.util.ClassNodeSource;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

public class SplashScreenFix extends InjectionWithContext<LegacyTweakContext> {

    public SplashScreenFix(LegacyTweakContext storage) {
        super(storage);
    }

    @Override
    public String name() {
        return "Splash screen fix";
    }

    @Override
	public boolean required() {
		return false;
	}

    @Override
    public boolean apply(ClassNodeSource source, LaunchConfig config) {
		for(MethodNode m : context.minecraft.methods) {
			boolean store2 = false;
			boolean store3 = false;
			for(AbstractInsnNode insn : m.instructions) {
				if(compareInsn(insn, ISTORE, 2)) {
					store2 = true;
				}
				if(compareInsn(insn, ISTORE, 3)) {
					store3 = true;
				}
				if(insn.getOpcode() == LDC) {
					LdcInsnNode ldc = (LdcInsnNode) insn;
					if(ldc.cst.equals("/title/mojang.png")) {
						AbstractInsnNode insn3 = ldc.getNext();
						while(insn3 != null) {
							AbstractInsnNode[] insns2 = fill(insn3, 15);
							if(store2 && store3
							&& compareInsn(insns2[0], ALOAD)
							&& compareInsn(insns2[1], GETFIELD, null, null, "I")
							&& compareInsn(insns2[2], ICONST_2)
							&& compareInsn(insns2[3], IDIV)
							&& compareInsn(insns2[4], ILOAD)
							&& compareInsn(insns2[5], ISUB)
							&& compareInsn(insns2[6], ICONST_2)
							&& compareInsn(insns2[7], IDIV)
							&& compareInsn(insns2[8], ALOAD)
							&& compareInsn(insns2[9], GETFIELD, null, null, "I")
							&& compareInsn(insns2[10], ICONST_2)
							&& compareInsn(insns2[11], IDIV)
							&& compareInsn(insns2[12], ILOAD)
							&& compareInsn(insns2[13], ISUB)
							&& compareInsn(insns2[14], ICONST_2)) {
								m.instructions.remove(insns2[2]);
								m.instructions.remove(insns2[3]);
								m.instructions.remove(insns2[10]);
								m.instructions.remove(insns2[11]);
								m.instructions.remove(insns2[0]);
								m.instructions.remove(insns2[8]);
								m.instructions.set(insns2[1], new VarInsnNode(ILOAD, 2));
								m.instructions.set(insns2[9], new VarInsnNode(ILOAD, 3));
                                return true;
							}
							insn3 = nextInsn(insn3);
						}
						break;
					}
				}
			}
		}
        return false;
    }
    
}
