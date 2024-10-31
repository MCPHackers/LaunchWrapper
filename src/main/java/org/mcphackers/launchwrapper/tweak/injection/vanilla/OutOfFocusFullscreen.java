package org.mcphackers.launchwrapper.tweak.injection.vanilla;

import static org.mcphackers.launchwrapper.util.asm.InsnHelper.*;
import static org.objectweb.asm.Opcodes.*;

import org.mcphackers.launchwrapper.LaunchConfig;
import org.mcphackers.launchwrapper.tweak.injection.InjectionWithContext;
import org.mcphackers.launchwrapper.tweak.injection.MinecraftGetter;
import org.mcphackers.launchwrapper.util.ClassNodeSource;
import org.mcphackers.launchwrapper.util.asm.NodeHelper;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * This patch makes it so game window stays fullscreen, even if you change focus via alt-tab
 * Part of LWJGLPatch, but for 1.6+ without need for deAWT
 */
public class OutOfFocusFullscreen extends InjectionWithContext<MinecraftGetter> {

	public OutOfFocusFullscreen(MinecraftGetter context) {
		super(context);
	}

	public String name() {
		return "Fullscreen fix";
	}

	public boolean required() {
		return false;
	}

	private MethodNode getTickMethod(MethodNode run) {
		ClassNode minecraft = context.getMinecraft();
		FieldNode running = context.getIsRunning();
		if (running == null) {
			return run;
		}
		AbstractInsnNode insn = getFirst(run.instructions);
		while (insn != null) {
			if (compareInsn(insn.getPrevious(), ALOAD) &&
				compareInsn(insn, INVOKESPECIAL, minecraft.name, null, "()V")) {
				MethodInsnNode invoke = (MethodInsnNode)insn;
				MethodNode testedMethod = NodeHelper.getMethod(minecraft, invoke.name, invoke.desc);
				if (testedMethod != null) {
					AbstractInsnNode insn2 = getFirst(testedMethod.instructions);
					while (insn2 != null) {
						// tick method up to 1.12.2 contains this call
						if (compareInsn(insn2, INVOKESTATIC, "org/lwjgl/opengl/Display", "isCloseRequested", "()Z")) {
							return testedMethod;
						}
						insn2 = nextInsn(insn2);
					}
				}
			}
			insn = nextInsn(insn);
		}
		return run;
	}

	public boolean apply(ClassNodeSource source, LaunchConfig config) {
		ClassNode minecraft = context.getMinecraft();
		MethodNode tick = getTickMethod(context.getRun());
		for (AbstractInsnNode insn = getFirst(tick.instructions); insn != null; insn = nextInsn(insn)) {
			AbstractInsnNode[] insns2 = fill(insn, 7);
			if (compareInsn(insns2[0], INVOKESTATIC, "org/lwjgl/opengl/Display", "isActive", "()Z") &&
				compareInsn(insns2[1], IFNE) &&
				compareInsn(insns2[2], ALOAD) &&
				compareInsn(insns2[3], GETFIELD, minecraft.name, null, "Z") &&
				compareInsn(insns2[4], IFEQ) &&
				compareInsn(insns2[5], ALOAD) &&
				compareInsn(insns2[6], INVOKEVIRTUAL, minecraft.name, null, "()V")) {
				tick.instructions.set(insn, new InsnNode(ICONST_1));
				return true;
			}
		}
		return false;
	}
}
