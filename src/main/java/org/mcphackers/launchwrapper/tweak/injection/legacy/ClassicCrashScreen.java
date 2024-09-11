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
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * Brings back an unused error screen which was previously used to catch fatal
 * exceptions in Minecraft Classic and Indev
 * 
 * Currently only tested to work between a1.1 and b1.5
 * Known versions where it doesn't work yet: a1.0.1_01 and b1.6.6
 * 
 * Note that some crashes related to rendering may break Tesselator which will make it stuck in an exception loop
 */
public class ClassicCrashScreen extends InjectionWithContext<LegacyTweakContext> {

	public ClassicCrashScreen(LegacyTweakContext context) {
		super(context);
	}

	@Override
	public String name() {
		return "Classic crash screen";
	}

	@Override
	public boolean required() {
		return false;
	}

	@Override
	public boolean apply(ClassNodeSource source, LaunchConfig config) {
		if (context.run == null) {
			return false;
		}
		AbstractInsnNode insn1 = context.run.instructions.getFirst();
		String fieldName = null;
		String fieldDesc = null;
		LabelNode start = null;
		AbstractInsnNode end = null;
		TryCatchBlockNode afterCatch = null;
		while (insn1 != null) {
			AbstractInsnNode[] insns1 = fill(insn1, 6);
			if (compareInsn(insns1[0], ALOAD, 0)
					&& compareInsn(insns1[1], GETFIELD, context.minecraft.name, context.running.name,
							context.running.desc)
					&& compareInsn(insns1[2], IFEQ)
					&& compareInsn(insns1[3], ALOAD, 0)
					&& compareInsn(insns1[4], GETFIELD, context.minecraft.name, null,
							"L" + context.minecraftApplet.name + ";")
					&& compareInsn(insns1[5], IFNULL)) {
				start = new LabelNode();
				context.run.instructions.insertBefore(insn1, start);
				JumpInsnNode jmp = (JumpInsnNode) insns1[2];
				end = previousInsn(jmp.label); // GOTO outside of loop
				// end = previousInsn(end); // GOTO to the beggining of loop
				for (TryCatchBlockNode tryCatch : context.run.tryCatchBlocks) {
					if (afterCatch == null && tryCatch.type != null && !tryCatch.type.startsWith("java/lang/")) {
						afterCatch = tryCatch;
					}
					// if ("java/lang/Throwable".equals(tryCatch.type)) {
					AbstractInsnNode testInsn = nextInsn(tryCatch.handler);
					while (tryCatch.end != testInsn && testInsn != null) {
						testInsn = nextInsn(testInsn);
						if (compareInsn(testInsn, ACONST_NULL) && compareInsn(nextInsn(testInsn), PUTFIELD)) {
							FieldInsnNode putfield = (FieldInsnNode) nextInsn(testInsn);
							fieldName = putfield.name;
							fieldDesc = putfield.desc;
						}
					}
					// }
				}
			}
			insn1 = nextInsn(insn1);
		}
		if (start == null || end == null) {
			return false;
		}
		if (fieldDesc == null || fieldName == null) {
			return false;
		}
		MethodNode setWorld = null;
		for (MethodNode m : context.minecraft.methods) {
			if (m.desc.equals("(" + fieldDesc + ")V")) {
				setWorld = m;
				break;
			}
		}
		if (setWorld == null) {
			return false;
		}
		ClassNode errScreen = null;
		MethodNode openScreen = null;
		for (MethodNode m : context.minecraft.methods) {
			if (m.desc.equals("()V")) {
				AbstractInsnNode insn = m.instructions.getFirst();
				if (insn != null && insn.getOpcode() == -1) {
					insn = nextInsn(insn);
				}
				if (!compareInsn(insn, INVOKESTATIC, "org/lwjgl/opengl/Display", "isActive", "()Z")) {
					continue;
				}
				AbstractInsnNode insn2 = insn;
				while (insn2 != null) {
					AbstractInsnNode[] insns2 = fill(insn2, 3);
					if (compareInsn(insns2[0], ALOAD, 0)
							&& compareInsn(insns2[1], ACONST_NULL)
							&& compareInsn(insns2[2], INVOKEVIRTUAL, context.minecraft.name, null, null)) {
						MethodInsnNode invoke = (MethodInsnNode) insns2[2];
						openScreen = NodeHelper.getMethod(context.minecraft, invoke.name, invoke.desc);
						break;
					}
					insn2 = nextInsn(insn2);
				}
				break;
			}
		}
		if (openScreen == null) {
			return false;
		}
		AbstractInsnNode insn = openScreen.instructions.getFirst();
		while (insn != null) {
			if (insn.getOpcode() == INSTANCEOF) {
				break;
			}
			insn = nextInsn(insn);
		}
		if (insn == null) {
			return false;
		}
		errScreen = source.getClass(((TypeInsnNode) insn).desc);

		if (errScreen == null) {
			return false;
		}
		MethodNode init = NodeHelper.getMethod(errScreen, "<init>", "(Ljava/lang/String;Ljava/lang/String;)V");
		if (init == null) {
			MethodNode newInit = new MethodNode(ACC_PUBLIC, "<init>", "(Ljava/lang/String;Ljava/lang/String;)V", null,
					null);
			InsnList insnList = newInit.instructions;
			insnList.add(new VarInsnNode(ALOAD, 0));
			insnList.add(new MethodInsnNode(INVOKESPECIAL, errScreen.superName, "<init>", "()V"));
			int c = 1;
			for (FieldNode f : errScreen.fields) {
				if (!f.desc.equals("Ljava/lang/String;")) {
					continue;
				}
				insnList.add(new VarInsnNode(ALOAD, 0));
				insnList.add(new VarInsnNode(ALOAD, c));
				insnList.add(new FieldInsnNode(PUTFIELD, errScreen.name, f.name, f.desc));
				if (c == 2) {
					break;
				}
				c++;
			}
			insnList.add(new InsnNode(RETURN));
			errScreen.methods.add(newInit);
		}
		source.overrideClass(errScreen);

		int n = getFreeIndex(context.run.instructions);
		InsnList handle = new InsnList();
		handle.add(new VarInsnNode(ASTORE, n));
		handle.add(new VarInsnNode(ALOAD, 0));
		handle.add(new InsnNode(ACONST_NULL));
		handle.add(new FieldInsnNode(PUTFIELD, context.minecraft.name, fieldName, fieldDesc));
		handle.add(new VarInsnNode(ALOAD, 0));
		handle.add(new InsnNode(ACONST_NULL));
		handle.add(new MethodInsnNode(INVOKEVIRTUAL, context.minecraft.name, setWorld.name, setWorld.desc));
		handle.add(new VarInsnNode(ALOAD, n));
		handle.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/Throwable", "printStackTrace", "()V"));
		handle.add(new VarInsnNode(ALOAD, 0));
		handle.add(new TypeInsnNode(NEW, errScreen.name));
		handle.add(new InsnNode(DUP));
		handle.add(new LdcInsnNode("Client error"));
		handle.add(new TypeInsnNode(NEW, "java/lang/StringBuilder"));
		handle.add(new InsnNode(DUP));
		handle.add(new MethodInsnNode(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V"));
		handle.add(new LdcInsnNode("The game broke! ["));
		handle.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/StringBuilder", "append",
				"(Ljava/lang/String;)Ljava/lang/StringBuilder;"));
		handle.add(new VarInsnNode(ALOAD, n));
		handle.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/Object", "toString", "()Ljava/lang/String;"));
		handle.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/StringBuilder", "append",
				"(Ljava/lang/String;)Ljava/lang/StringBuilder;"));
		handle.add(new LdcInsnNode("]"));
		handle.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/StringBuilder", "append",
				"(Ljava/lang/String;)Ljava/lang/StringBuilder;"));
		handle.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;"));
		handle.add(
				new MethodInsnNode(INVOKESPECIAL, errScreen.name, "<init>", "(Ljava/lang/String;Ljava/lang/String;)V"));
		handle.add(new MethodInsnNode(INVOKEVIRTUAL, context.minecraft.name, openScreen.name, openScreen.desc));

		handle.add(new JumpInsnNode(GOTO, start));
		addTryCatch(context.run, start, end, handle, "java/lang/Exception", afterCatch);
		source.overrideClass(context.minecraft);
		return true;
	}

}
