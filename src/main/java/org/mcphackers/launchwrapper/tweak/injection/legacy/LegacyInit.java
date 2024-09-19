package org.mcphackers.launchwrapper.tweak.injection.legacy;

import static org.mcphackers.rdi.util.InsnHelper.*;
import static org.objectweb.asm.Opcodes.*;

import org.mcphackers.launchwrapper.LaunchConfig;
import org.mcphackers.launchwrapper.tweak.LegacyTweak;
import org.mcphackers.launchwrapper.tweak.injection.InjectionWithContext;
import org.mcphackers.launchwrapper.tweak.storage.LegacyTweakContext;
import org.mcphackers.launchwrapper.util.ClassNodeSource;
import org.mcphackers.rdi.util.NodeHelper;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Initializes LegacyTweakContext
 */
public class LegacyInit extends InjectionWithContext<LegacyTweakContext> {

    public LegacyInit(LegacyTweakContext storage) {
        super(storage);
    }

    @Override
	public String name() {
		return "LegacyTweak init";
	}

    @Override
	public boolean required() {
		return true;
	}

    @Override
    public boolean apply(ClassNodeSource source, LaunchConfig config) {
		context.minecraftApplet = getApplet(source);
		context.minecraft = getMinecraft(source, context.minecraftApplet);
		if(context.minecraft == null) {
			return false;
		}
		context.run = NodeHelper.getMethod(context.minecraft, "run", "()V");
		if(context.run == null) {
			return false;
		}
		for(MethodNode method : context.minecraft.methods) {
			if("main".equals(method.name) && "([Ljava/lang/String;)V".equals(method.desc) && (method.access & Opcodes.ACC_STATIC) != 0) {
				context.main = method;
			}
			if("run".equals(method.name) && "()V".equals(method.desc)) {
				AbstractInsnNode insn = method.instructions.getFirst();
				while(insn != null) {
					if(insn.getOpcode() == Opcodes.PUTFIELD) {
						FieldInsnNode putField = (FieldInsnNode) insn;
						if("Z".equals(putField.desc)) {
							context.running = NodeHelper.getField(context.minecraft, putField.name, putField.desc);
						}
						break;
					}
					insn = nextInsn(insn);
				}
			}
		}
		int i = 0;
		boolean previousIsWidth = false;
		FieldNode width = null;
		FieldNode height = null;
		for(FieldNode field : context.minecraft.fields) {
			if("I".equals(field.desc) && (field.access & ACC_STATIC) == 0) {
				// Width and height are always the first two NON-STATIC int fields in Minecraft class
				if(i == 0) {
					previousIsWidth = true;
					width = field;
				}
				if(i == 1 && previousIsWidth)
					height = field;
				i++;
			} else {
				previousIsWidth = false;
			}
			if("Ljava/io/File;".equals(field.desc)) {
				context.mcDir = field; // Possible candidate (Needed for infdev)
				if((field.access & Opcodes.ACC_STATIC) != 0) {
					// Definitely the mc directory
					break;
				}
			}
		}
		if(width != null && height != null) {
			context.width = width;
			context.height = height;
		}
		if(context.minecraftApplet != null) {
			String mcDesc = "L" + context.minecraft.name + ";";
			String mcField = null;
			for(FieldNode field : context.minecraftApplet.fields) {
				if(mcDesc.equals(field.desc)) {
					mcField = field.name;
				}
			}

			MethodNode init = NodeHelper.getMethod(context.minecraftApplet, "init", "()V");
			if(init != null) {
				AbstractInsnNode insn = init.instructions.getFirst();
				while(insn != null) {
					AbstractInsnNode[] insns = fill(insn, 8);
					if(compareInsn(insns[0], ALOAD)
					&& compareInsn(insns[1], GETFIELD, context.minecraftApplet.name, mcField, mcDesc)
					&& compareInsn(insns[2], ICONST_1)
					&& compareInsn(insns[3], PUTFIELD, context.minecraft.name, null, "Z")) {
						FieldInsnNode field = (FieldInsnNode) insns[3];
						context.appletMode = NodeHelper.getField(context.minecraft, field.name, field.desc);
						break;
					}
					if(compareInsn(insns[0], ALOAD)
					&& compareInsn(insns[1], GETFIELD, context.minecraftApplet.name, mcField, mcDesc)
					&& compareInsn(insns[2], LDC, "true")
					&& compareInsn(insns[3], ALOAD)
					&& compareInsn(insns[4], LDC, "stand-alone")
					&& compareInsn(insns[5], INVOKEVIRTUAL, context.minecraftApplet.name, "getParameter", "(Ljava/lang/String;)Ljava/lang/String;")
					&& compareInsn(insns[6], INVOKEVIRTUAL, "java/lang/String", "equals", "(Ljava/lang/Object;)Z")) {
						AbstractInsnNode[] insns2 = fill(insns[7], 7);
						if(compareInsn(insns2[0], IFNE)
						&& compareInsn(insns2[1], ICONST_1)
						&& compareInsn(insns2[2], GOTO)
						&& compareInsn(insns2[3], ICONST_0)
						&& compareInsn(insns2[4], PUTFIELD, context.minecraft.name, null, "Z")) {
							FieldInsnNode field = (FieldInsnNode) insns2[4];
							context.appletMode = NodeHelper.getField(context.minecraft, field.name, field.desc);
							break;
						}
						if(compareInsn(insns2[0], IFEQ)
						&& compareInsn(insns2[1], ICONST_0)
						&& compareInsn(insns2[2], GOTO)
						&& compareInsn(insns2[3], ICONST_1)
						&& compareInsn(insns2[4], PUTFIELD, context.minecraft.name, null, "Z")) {
							FieldInsnNode field = (FieldInsnNode) insns2[4];
							context.appletMode = NodeHelper.getField(context.minecraft, field.name, field.desc);
							break;
						}
					}
					insn = nextInsn(insn);
				}
			}
		}
		if(config.forceResizable.get()) {
			context.supportsResizing = true;
		}
		source.overrideClass(context.minecraft);
        return true;
	}

	public ClassNode getApplet(ClassNodeSource source) {
		ClassNode applet = null;
		for(String main : LegacyTweak.MAIN_APPLETS) {
			applet = source.getClass(main);
			if(applet != null)
				break;
		}
		return applet;
	}

	public ClassNode getMinecraft(ClassNodeSource source, ClassNode applet) {
		ClassNode launchTarget = null;
		for(String main : LegacyTweak.MAIN_CLASSES) {
			ClassNode cls = source.getClass(main);
			if(cls != null && cls.interfaces.contains("java/lang/Runnable")) {
				launchTarget = cls;
				break;
			}
		}
		if(launchTarget == null && applet != null) {
			for(FieldNode field : applet.fields) {
				String desc = field.desc;
				if(!desc.equals("Ljava/awt/Canvas;") && !desc.equals("Ljava/lang/Thread;") && desc.startsWith("L") && desc.endsWith(";")) {
					launchTarget = source.getClass(desc.substring(1, desc.length() - 1));
				}
			}
		}
		return launchTarget;
	}
    
}
