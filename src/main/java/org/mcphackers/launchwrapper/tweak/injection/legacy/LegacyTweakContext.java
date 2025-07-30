package org.mcphackers.launchwrapper.tweak.injection.legacy;

import static org.mcphackers.launchwrapper.util.asm.InsnHelper.*;
import static org.objectweb.asm.Opcodes.*;

import org.mcphackers.launchwrapper.LaunchConfig;
import org.mcphackers.launchwrapper.tweak.LegacyTweak;
import org.mcphackers.launchwrapper.tweak.injection.Injection;
import org.mcphackers.launchwrapper.tweak.injection.MinecraftGetter;
import org.mcphackers.launchwrapper.util.ClassNodeSource;
import org.mcphackers.launchwrapper.util.asm.NodeHelper;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Initializes context for LegacyTweak
 */
public class LegacyTweakContext implements Injection, MinecraftGetter {
	private ClassNode minecraft;
	private ClassNode minecraftApplet;
	/**
	 * Field that determines if Minecraft should exit
	 */
	private FieldNode running;
	/**
	 * Frame width
	 */
	public FieldNode width;
	/**
	 * Frame height
	 */
	public FieldNode height;
	/**
	 * Working game directory
	 */
	public FieldNode mcDir;
	/**
	 * Whenever the game runs in an applet
	 */
	public FieldNode appletMode;
	/**
	 * Default width when toggling fullscreen
	 */
	public FieldInsnNode defaultWidth;
	/**
	 * Default height when toggling fullscreen
	 */
	public FieldInsnNode defaultHeight;
	public FieldInsnNode fullscreenField;
	public boolean supportsResizing;
	private MethodNode run;

	public ClassNode getMinecraft() {
		return minecraft;
	}

	public MethodNode getRun() {
		return run;
	}

	public FieldNode getIsRunning() {
		return running;
	}

	public MethodNode getInit() {
		for (AbstractInsnNode insn : run.instructions) {
			if (insn.getType() == AbstractInsnNode.METHOD_INSN) {
				MethodInsnNode invoke = (MethodInsnNode)insn;
				if (invoke.owner.equals(minecraft.name)) {
					return NodeHelper.getMethod(minecraft, invoke.name, invoke.desc);
				} else {
					// Sometimes init() is inlined inside of run() method
					return run;
				}
			}
		}
		return run;
	}

	public ClassNode getMinecraftApplet() {
		return minecraftApplet;
	}

	public boolean isClassic() {
		for (String s : LegacyTweak.MAIN_CLASSES) {
			if (s.equals("net/minecraft/client/Minecraft")) {
				continue;
			}
			if (s.equals(minecraft.name)) {
				return true;
			}
		}
		if (minecraftApplet != null) {
			for (String s : LegacyTweak.MAIN_APPLETS) {
				if (s.equals("net/minecraft/client/MinecraftApplet")) {
					continue;
				}
				if (s.equals(minecraftApplet.name)) {
					return true;
				}
			}
		}
		return false;
	}

	public String name() {
		return "LegacyTweak init";
	}

	public boolean required() {
		return true;
	}

	public boolean apply(ClassNodeSource source, LaunchConfig config) {
		minecraftApplet = getApplet(source);
		minecraft = getMinecraft(source, minecraftApplet);
		if (minecraft == null) {
			return false;
		}
		run = NodeHelper.getMethod(minecraft, "run", "()V");
		if (run == null) {
			return false;
		}
		for (MethodNode method : minecraft.methods) {
			if ("run".equals(method.name) && "()V".equals(method.desc)) {
				AbstractInsnNode insn = getFirst(method.instructions);
				while (insn != null) {
					if (insn.getOpcode() == Opcodes.PUTFIELD) {
						FieldInsnNode putField = (FieldInsnNode)insn;
						if ("Z".equals(putField.desc)) {
							running = NodeHelper.getField(minecraft, putField.name, putField.desc);
						}
						break;
					}
					insn = nextInsn(insn);
				}
			}
		}
		int i = 0;
		boolean previousIsWidth = false;
		for (FieldNode field : minecraft.fields) {
			if ("I".equals(field.desc) && (field.access & ACC_STATIC) == 0) {
				// Width and height are always the first two NON-STATIC int fields in Minecraft class
				if (i == 0) {
					previousIsWidth = true;
					width = field;
				}
				if (i == 1 && previousIsWidth) {
					height = field;
				}
				i++;
			} else {
				previousIsWidth = false;
			}
			if ("Ljava/io/File;".equals(field.desc)) {
				mcDir = field; // Possible candidate (Needed for infdev)
				if ((field.access & Opcodes.ACC_STATIC) != 0) {
					// Definitely the mc directory
					break;
				}
			}
		}
		if (minecraftApplet != null) {
			String mcDesc = "L" + minecraft.name + ";";
			String mcField = null;
			for (FieldNode field : minecraftApplet.fields) {
				if (mcDesc.equals(field.desc)) {
					mcField = field.name;
				}
			}

			MethodNode init = NodeHelper.getMethod(minecraftApplet, "fmlInitReentry", "()V");
			if (init == null) {
				init = NodeHelper.getMethod(minecraftApplet, "init", "()V");
			}
			if (init != null) {
				AbstractInsnNode insn = getFirst(init.instructions);
				while (insn != null) {
					AbstractInsnNode[] insns = fill(insn, 8);
					if (compareInsn(insns[0], ALOAD) &&
						compareInsn(insns[1], GETFIELD, minecraftApplet.name, mcField, mcDesc) &&
						compareInsn(insns[2], ICONST_1) &&
						compareInsn(insns[3], PUTFIELD, minecraft.name, null, "Z")) {
						FieldInsnNode field = (FieldInsnNode)insns[3];
						appletMode = NodeHelper.getField(minecraft, field.name, field.desc);
						break;
					}
					if (compareInsn(insns[0], ALOAD) &&
						compareInsn(insns[1], GETFIELD, minecraftApplet.name, mcField, mcDesc) &&
						compareInsn(insns[2], LDC, "true") &&
						compareInsn(insns[3], ALOAD) &&
						compareInsn(insns[4], LDC, "stand-alone") &&
						compareInsn(insns[5], INVOKEVIRTUAL, minecraftApplet.name, "getParameter", "(Ljava/lang/String;)Ljava/lang/String;") &&
						compareInsn(insns[6], INVOKEVIRTUAL, "java/lang/String", "equals", "(Ljava/lang/Object;)Z")) {
						AbstractInsnNode[] insns2 = fill(insns[7], 7);
						if (compareInsn(insns2[0], IFNE) &&
							compareInsn(insns2[1], ICONST_1) &&
							compareInsn(insns2[2], GOTO) &&
							compareInsn(insns2[3], ICONST_0) &&
							compareInsn(insns2[4], PUTFIELD, minecraft.name, null, "Z")) {
							FieldInsnNode field = (FieldInsnNode)insns2[4];
							appletMode = NodeHelper.getField(minecraft, field.name, field.desc);
							break;
						}
						if (compareInsn(insns2[0], IFEQ) &&
							compareInsn(insns2[1], ICONST_0) &&
							compareInsn(insns2[2], GOTO) &&
							compareInsn(insns2[3], ICONST_1) &&
							compareInsn(insns2[4], PUTFIELD, minecraft.name, null, "Z")) {
							FieldInsnNode field = (FieldInsnNode)insns2[4];
							appletMode = NodeHelper.getField(minecraft, field.name, field.desc);
							break;
						}
					}
					insn = nextInsn(insn);
				}
			}
		}
		if (config.resizable.get()) {
			supportsResizing = true;
		}
		source.overrideClass(minecraft);
		return true;
	}

	public ClassNode getApplet(ClassNodeSource source) {
		ClassNode applet = null;
		for (String main : LegacyTweak.MAIN_APPLETS) {
			applet = source.getClass(main);
			if (applet != null) {
				break;
			}
		}
		return applet;
	}

	public ClassNode getMinecraft(ClassNodeSource source, ClassNode applet) {
		ClassNode launchTarget = null;
		for (String main : LegacyTweak.MAIN_CLASSES) {
			ClassNode cls = source.getClass(main);
			if (cls != null && cls.interfaces.contains("java/lang/Runnable")) {
				launchTarget = cls;
				break;
			}
		}
		if (launchTarget == null && applet != null) {
			for (FieldNode field : applet.fields) {
				String desc = field.desc;
				if (!desc.equals("Ljava/awt/Canvas;") && !desc.equals("Ljava/lang/Thread;") && desc.startsWith("L") && desc.endsWith(";")) {
					launchTarget = source.getClass(desc.substring(1, desc.length() - 1));
				}
			}
		}
		return launchTarget;
	}
}
