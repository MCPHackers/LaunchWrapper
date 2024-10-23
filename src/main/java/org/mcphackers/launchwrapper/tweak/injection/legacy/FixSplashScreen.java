package org.mcphackers.launchwrapper.tweak.injection.legacy;

import static org.mcphackers.launchwrapper.util.asm.InsnHelper.*;
import static org.objectweb.asm.Opcodes.*;

import org.mcphackers.launchwrapper.LaunchConfig;
import org.mcphackers.launchwrapper.tweak.injection.InjectionWithContext;
import org.mcphackers.launchwrapper.tweak.injection.MinecraftGetter;
import org.mcphackers.launchwrapper.util.ClassNodeSource;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * Fixes mojang splash screen offset when running with custom window size or fullscreen
 */
public class FixSplashScreen extends InjectionWithContext<MinecraftGetter> {

	public FixSplashScreen(MinecraftGetter storage) {
		super(storage);
	}

	public String name() {
		return "Splash screen fix";
	}

	public boolean required() {
		return false;
	}

	public boolean apply(ClassNodeSource source, LaunchConfig config) {
		ClassNode minecraft = context.getMinecraft();
		for (MethodNode m : minecraft.methods) {
			boolean astore1 = false;
			boolean store2 = false;
			boolean store3 = false;
			String owner = null;
			String fieldWidth = null;
			String fieldHeight = null;
			for (AbstractInsnNode insn : m.instructions) {
				if (compareInsn(insn, ASTORE, 1)) {
					// stores ScreenSizeCalculator, or whatever is used for scaling of Screen instances
					astore1 = true;
				}
				if (astore1 && fieldWidth == null && fieldHeight == null) {
					AbstractInsnNode[] insns2 = fill(insn, 4);
					if (compareInsn(insns2[0], ALOAD, 1) &&
						compareInsn(insns2[1], GETFIELD, null, null, "D") &&
						compareInsn(insns2[2], ALOAD, 1) &&
						compareInsn(insns2[3], GETFIELD, null, null, "D")) {
						FieldInsnNode width = (FieldInsnNode)insns2[1];
						FieldInsnNode height = (FieldInsnNode)insns2[3];
						owner = width.owner;
						fieldWidth = width.name;
						fieldHeight = height.name;
					}
				}
				if (compareInsn(insn, ISTORE, 2)) {
					// stores width from ScreenSizeCalculator. (Only in b1.3 or prior)
					store2 = true;
				}
				if (compareInsn(insn, ISTORE, 3)) {
					// stores height from ScreenSizeCalculator. (Only in b1.3 or prior)
					store3 = true;
				}
				if (insn.getOpcode() == LDC) {
					LdcInsnNode ldc = (LdcInsnNode)insn;
					if (ldc.cst.equals("/title/mojang.png")) {
						AbstractInsnNode insn3 = ldc.getNext();
						while (insn3 != null) {
							AbstractInsnNode[] insns2 = fill(insn3, 15);
							if ((store2 && store3 || owner != null) &&
								compareInsn(insns2[0], ALOAD) &&
								compareInsn(insns2[1], GETFIELD, null, null, "I") &&
								compareInsn(insns2[2], ICONST_2) &&
								compareInsn(insns2[3], IDIV) &&
								compareInsn(insns2[4], ILOAD) &&
								compareInsn(insns2[5], ISUB) &&
								compareInsn(insns2[6], ICONST_2) &&
								compareInsn(insns2[7], IDIV) &&
								compareInsn(insns2[8], ALOAD) &&
								compareInsn(insns2[9], GETFIELD, null, null, "I") &&
								compareInsn(insns2[10], ICONST_2) &&
								compareInsn(insns2[11], IDIV) &&
								compareInsn(insns2[12], ILOAD) &&
								compareInsn(insns2[13], ISUB) &&
								compareInsn(insns2[14], ICONST_2)) {
								if (store2 && store3 || owner != null) {
									m.instructions.remove(insns2[2]);
									m.instructions.remove(insns2[3]);
									m.instructions.remove(insns2[10]);
									m.instructions.remove(insns2[11]);
								}
								if (store2 && store3) {
									// b1.3 and prior
									m.instructions.remove(insns2[0]);
									m.instructions.set(insns2[1], new VarInsnNode(ILOAD, 2));
									m.instructions.remove(insns2[8]);
									m.instructions.set(insns2[9], new VarInsnNode(ILOAD, 3));
									source.overrideClass(minecraft);
									return true;
								} else if (owner != null) {
									// b1.4
									InsnList list = new InsnList();
									list.add(new IntInsnNode(ALOAD, 1));
									list.add(new FieldInsnNode(GETFIELD, owner, fieldWidth, "D"));
									list.add(new MethodInsnNode(INVOKESTATIC, "java/lang/Math", "ceil", "(D)D"));
									list.add(new InsnNode(D2I));
									m.instructions.insertBefore(insns2[0], list);
									m.instructions.remove(insns2[0]);
									m.instructions.remove(insns2[1]);

									list = new InsnList();
									list.add(new IntInsnNode(ALOAD, 1));
									list.add(new FieldInsnNode(GETFIELD, owner, fieldHeight, "D"));
									list.add(new MethodInsnNode(INVOKESTATIC, "java/lang/Math", "ceil", "(D)D"));
									list.add(new InsnNode(D2I));
									m.instructions.insertBefore(insns2[8], list);
									m.instructions.remove(insns2[8]);
									m.instructions.remove(insns2[9]);
									source.overrideClass(minecraft);
									return true;
								} else {
									return false;
								}
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
