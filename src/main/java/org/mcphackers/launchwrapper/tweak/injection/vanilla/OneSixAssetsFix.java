package org.mcphackers.launchwrapper.tweak.injection.vanilla;

import static org.mcphackers.rdi.util.InsnHelper.*;
import static org.objectweb.asm.Opcodes.*;

import org.mcphackers.launchwrapper.LaunchConfig;
import org.mcphackers.launchwrapper.tweak.injection.InjectionWithContext;
import org.mcphackers.launchwrapper.tweak.storage.VanillaTweakContext;
import org.mcphackers.launchwrapper.util.ClassNodeSource;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;

public class OneSixAssetsFix extends InjectionWithContext<VanillaTweakContext> {

    public OneSixAssetsFix(VanillaTweakContext storage) {
        super(storage);
    }

    @Override
    public String name() {
        return "1.6 Assets directory";
    }

    @Override
	public boolean required() {
		return false;
	}

    @Override
    public boolean apply(ClassNodeSource source, LaunchConfig config) {
		if(context.availableParameters.contains("assetsDir") || config.assetsDir.get() == null) {
            return false;
        }
        boolean success = false;
		for(MethodNode m : context.minecraft.methods) {
			AbstractInsnNode insn = m.instructions.getFirst();
			while(insn != null) {
				AbstractInsnNode[] insns = fill(insn, 6);
				if(compareInsn(insns[0], NEW, "java/io/File")
				&& compareInsn(insns[1], DUP)
				&& compareInsn(insns[2], ALOAD, 0)
				&& compareInsn(insns[3], GETFIELD, context.minecraft.name, null, "Ljava/io/File;")
				&& compareInsn(insns[4], LDC)
				&& compareInsn(insns[5], INVOKESPECIAL, "java/io/File", "<init>", "(Ljava/io/File;Ljava/lang/String;)V")) {
					LdcInsnNode ldc = (LdcInsnNode) insns[4];
					String s = (String) ldc.cst;
					if(s.startsWith("assets/")) {
						ldc.cst = s.substring(6);
						InsnList insert = new InsnList();
						insert.add(new TypeInsnNode(NEW, "java/io/File"));
						insert.add(new InsnNode(DUP));
						insert.add(new LdcInsnNode(config.assetsDir.getString()));
						insert.add(new MethodInsnNode(INVOKESPECIAL, "java/io/File", "<init>", "(Ljava/lang/String;)V"));
						m.instructions.remove(insns[2]);
						m.instructions.remove(insns[3]);
						m.instructions.insert(insns[1], insert);
                        success = true;
					} else if(s.equals("assets")) {
						ldc.cst = config.assetsDir.getString();
						m.instructions.remove(insns[2]);
						m.instructions.remove(insns[3]);
						m.instructions.set(insns[5], new MethodInsnNode(INVOKESPECIAL, "java/io/File", "<init>", "(Ljava/lang/String;)V"));
                        success = true;
					}
				}
				insn = nextInsn(insn);
			}
		}
        return success;
    }
    
}
