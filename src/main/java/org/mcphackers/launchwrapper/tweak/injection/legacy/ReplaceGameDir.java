package org.mcphackers.launchwrapper.tweak.injection.legacy;

import static org.mcphackers.rdi.util.InsnHelper.*;
import static org.objectweb.asm.Opcodes.*;

import org.mcphackers.launchwrapper.LaunchConfig;
import org.mcphackers.launchwrapper.tweak.injection.InjectionWithContext;
import org.mcphackers.launchwrapper.tweak.storage.LegacyTweakContext;
import org.mcphackers.launchwrapper.util.ClassNodeSource;
import org.mcphackers.rdi.util.NodeHelper;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * Patches versions to use directory specified by LaunchWrapper as the game directory
 */
public class ReplaceGameDir extends InjectionWithContext<LegacyTweakContext> {

    public ReplaceGameDir(LegacyTweakContext storage) {
        super(storage);
    }

    @Override
    public String name() {
        return "Replace game directory";
    }

    @Override
	public boolean required() {
		return true;
	}

    @Override
    public boolean apply(ClassNodeSource source, LaunchConfig config) {
        return replaceGameDirectory(context.getInit(), context.mcDir, config);
    }

	private InsnList getGameDirectory(LaunchConfig config) {
		InsnList insns = new InsnList();
		insns.add(new TypeInsnNode(NEW, "java/io/File"));
		insns.add(new InsnNode(DUP));
		insns.add(new LdcInsnNode(config.gameDir.getString()));
		insns.add(new MethodInsnNode(INVOKESPECIAL, "java/io/File", "<init>", "(Ljava/lang/String;)V"));
		return insns;
	}

	public boolean replaceGameDirectory(MethodNode init, FieldNode mcDir, LaunchConfig config) {
		boolean found = false;
        InsnList insnList = init.instructions;
		AbstractInsnNode insn = insnList.getFirst();
		while(insn != null) {
			AbstractInsnNode[] insns = fill(insn, 6);
			// Indev game dir patch
			if(mcDir != null && config.gameDir.get() != null
			&& compareInsn(insns[1], PUTFIELD, context.minecraft.name, mcDir.name, mcDir.desc)
			&& compareInsn(insns[0], ALOAD)) {
				insnList.remove(insns[0]);
				insnList.insertBefore(insns[1], getGameDirectory(config));
				insn = insns[1];
                found = true;
			}
			// Classic game dir patch
			if(mcDir == null
			&& compareInsn(insns[0], ALOAD)
			&& compareInsn(insns[1], INVOKEVIRTUAL, "java/io/File", "exists", "()Z")
			&& compareInsn(insns[2], IFNE)
			&& compareInsn(insns[3], ALOAD)
			&& compareInsn(insns[4], INVOKEVIRTUAL, "java/io/File", "mkdirs", "()Z")
			&& compareInsn(insns[5], IFNE)) {
				LabelNode lbl = ((JumpInsnNode) insns[2]).label;
				int index = ((VarInsnNode) insns[0]).var;

				if(lbl == ((JumpInsnNode) insns[5]).label && index == ((VarInsnNode) insns[3]).var) {
					AbstractInsnNode[] insns2 = fill(nextInsn(lbl), 2);
					if(compareInsn(insns2[0], ALOAD)
					&& compareInsn(insns2[1], ASTORE)) {
						if(index == ((VarInsnNode) insns2[0]).var) {
							insnList.remove(insns2[0]);
							insnList.insertBefore(insns2[1], getGameDirectory(config));
                            found = true;
						}
					}
				}
			}
			insn = nextInsn(insn);
		}
		if(mcDir != null && config.gameDir.get() != null) {
			if(NodeHelper.isStatic(mcDir)) {
				MethodNode clinit = NodeHelper.getMethod(context.minecraft, "<clinit>", "()V");
				if(clinit != null) {
					InsnList insns = new InsnList();
					insns.add(getGameDirectory(config));
					insns.add(new FieldInsnNode(PUTSTATIC, context.minecraft.name, mcDir.name, mcDir.desc));
					clinit.instructions.insertBefore(getLastReturn(clinit.instructions.getLast()), insns);
                    found = true;
				}
			} else {
				for(MethodNode m : context.minecraft.methods) {
					if(m.name.equals("<init>")) {
						InsnList insns = new InsnList();
						insns.add(new VarInsnNode(ALOAD, 0));
						insns.add(getGameDirectory(config));
						insns.add(new FieldInsnNode(PUTFIELD, context.minecraft.name, mcDir.name, mcDir.desc));
						m.instructions.insertBefore(getLastReturn(m.instructions.getLast()), insns);
                        found = true;
					}
				}
			}
		}
        return found;
	}
    
}
