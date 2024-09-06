package org.mcphackers.launchwrapper.tweak.injection.legacy;

import static org.mcphackers.launchwrapper.util.InsnHelper.*;
import static org.mcphackers.rdi.util.InsnHelper.*;
import static org.objectweb.asm.Opcodes.*;

import org.mcphackers.launchwrapper.LaunchConfig;
import org.mcphackers.launchwrapper.tweak.injection.InjectionWithContext;
import org.mcphackers.launchwrapper.util.ClassNodeSource;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodNode;

public class FixGrayScreen extends InjectionWithContext<LegacyTweakContext> {

    public FixGrayScreen(LegacyTweakContext storage) {
        super(storage);
    }

    @Override
    public String name() {
        return "Fix gray screen";
    }

    @Override
	public boolean required() {
		return false;
	}

    @Override
    public boolean apply(ClassNodeSource source, LaunchConfig config) {
        
		for(MethodNode m : context.minecraft.methods) {
			if(m.desc.equals("()V")) {
				AbstractInsnNode insn = m.instructions.getFirst();
				while(insn != null) {
					if(compareInsn(insn, INVOKESTATIC, "org/lwjgl/opengl/Display", "setDisplayConfiguration", "(FFF)V")) {
						AbstractInsnNode[] insns = fillBackwards(insn, 4);
						if(compareInsn(insns[0], FCONST_1)
						&& compareInsn(insns[1], FCONST_0)
						&& compareInsn(insns[2], FCONST_0)) {
							m.instructions.insertBefore(insns[0], new InsnNode(NOP));
							removeRange(m.instructions, insns[0], insn);
							return true;
						}
					}
					if(insn.getOpcode() == INVOKESTATIC) {
						return false;
					}
					insn = nextInsn(insn);
				}
			}
		}
        return false;

    }
    
    
}
