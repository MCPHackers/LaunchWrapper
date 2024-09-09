package org.mcphackers.launchwrapper.tweak.injection.legacy;

import static org.mcphackers.rdi.util.InsnHelper.*;
import static org.objectweb.asm.Opcodes.*;

import org.mcphackers.launchwrapper.LaunchConfig;
import org.mcphackers.launchwrapper.tweak.injection.InjectionWithContext;
import org.mcphackers.launchwrapper.util.ClassNodeSource;
import org.mcphackers.rdi.util.NodeHelper;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

public class OptionsLoadFix extends InjectionWithContext<LegacyTweakContext> {
    
	public OptionsLoadFix(LegacyTweakContext storage) {
        super(storage);
    }

    @Override
    public String name() {
        return "Options load fix";
    }

    @Override
	public boolean required() {
		return false;
	}

    @Override
    public boolean apply(ClassNodeSource source, LaunchConfig config) {
        return optionsLoadFix(source, context.getInit());
    }

    private boolean optionsLoadFix(ClassNodeSource source, MethodNode init) {
		AbstractInsnNode insn = init.instructions.getFirst();
		while(insn != null) {
			AbstractInsnNode[] insns = fill(insn, 8);
			if(compareInsn(insns[0], ALOAD)
			&& compareInsn(insns[1], NEW)
			&& compareInsn(insns[2], DUP)
			&& compareInsn(insns[3], ALOAD)
			&& compareInsn(insns[4], ALOAD)
			&& compareInsn(insns[5], GETFIELD, context.minecraft.name, null, "Ljava/io/File;")
			&& compareInsn(insns[6], INVOKESPECIAL, null, "<init>")
			&& compareInsn(insns[7], PUTFIELD, context.minecraft.name)) {
				MethodInsnNode invoke = (MethodInsnNode) insns[6];
				ClassNode optionsClass = source.getClass(invoke.owner);
				if(optionsClass != null) {
					MethodNode optionsInit = NodeHelper.getMethod(optionsClass, invoke.name, invoke.desc);
					if(optionsInit != null) {
						boolean isOptions = false;
						MethodInsnNode invoke2 = null;
						AbstractInsnNode insn2 = optionsInit.instructions.getLast();
						while(insn2 != null) {
							if(compareInsn(insn2, LDC, "options.txt")) {
								isOptions = true;
								break;
							}
							if(compareInsn(insn2, INVOKEVIRTUAL, optionsClass.name, null, "()V")
							|| compareInsn(insn2, INVOKESPECIAL, optionsClass.name, null, "()V")) {
								invoke2 = (MethodInsnNode) insn2;
							}
							insn2 = previousInsn(insn2);
						}
						if(isOptions && invoke2 != null) {
							MethodNode optionsInit2 = NodeHelper.getMethod(optionsClass, invoke2.name, invoke2.desc);

							for(TryCatchBlockNode tryCatch : optionsInit2.tryCatchBlocks) {
								AbstractInsnNode insn3 = nextInsn(tryCatch.start);
								AbstractInsnNode[] insns3 = fill(insn3, 4);
								if(compareInsn(insns3[0], ALOAD)
								&& compareInsn(insns3[1], LDC, ":")
								&& compareInsn(insns3[2], INVOKEVIRTUAL, "java/lang/String", "split", "(Ljava/lang/String;)[Ljava/lang/String;")
								&& compareInsn(insns3[3], ASTORE)) {
									return false;
								}
							}
							AbstractInsnNode insn3 = optionsInit2.instructions.getFirst();
							LabelNode lbl = null;
							VarInsnNode var0 = null;
							while(insn3 != null) {
								AbstractInsnNode[] insns3 = fill(insn3, 5);
								if(compareInsn(insns3[0], ALOAD)
								&& compareInsn(insns3[1], INVOKEVIRTUAL, "java/io/BufferedReader", "readLine", "()Ljava/lang/String;")
								&& compareInsn(insns3[2], DUP)
								&& compareInsn(insns3[3], ASTORE)) {
									lbl = labelBefore(insn3);
									var0 = (VarInsnNode) insns3[3];
								}

								if(compareInsn(insns3[0], ALOAD)
								&& compareInsn(insns3[1], LDC, ":")
								&& compareInsn(insns3[2], INVOKEVIRTUAL, "java/lang/String", "split", "(Ljava/lang/String;)[Ljava/lang/String;")) {
									if(lbl == null) {
										return false;
									}
									if(compareInsn(insns3[3], DUP)
									&& compareInsn(insns3[4], ASTORE)) {
										VarInsnNode var1 = (VarInsnNode) insns3[4];
										if(var1.var == var0.var) {
											VarInsnNode aload = (VarInsnNode) insns3[0];
											aload.var = var0.var = getFreeIndex(optionsInit2.instructions);
										}
									}
									AbstractInsnNode insn4 = insns3[0];
									while(insn4 != null) {
										if(compareInsn(insn4, GOTO, lbl)) {
											InsnList handle = new InsnList();
											handle.add(new InsnNode(POP));
											handle.add(new FieldInsnNode(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;"));
											handle.add(new TypeInsnNode(NEW, "java/lang/StringBuilder"));
											handle.add(new InsnNode(DUP));
											handle.add(new MethodInsnNode(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V"));
											handle.add(new LdcInsnNode("Skipping bad option: "));
											handle.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;"));
											handle.add(new VarInsnNode(ALOAD, var0.var));
											handle.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;"));
											handle.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;"));
											handle.add(new MethodInsnNode(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V"));
											addTryCatch(optionsInit2, insn3, insn4.getPrevious(), handle, "java/lang/Exception");
											source.overrideClass(optionsClass);
											return true;
										}
										insn4 = nextInsn(insn4);
									}
								}
								insn3 = nextInsn(insn3);
							}
						}
					}
				}
				break;
			}
			insn = nextInsn(insn);
		}
        return false;
	}
}
