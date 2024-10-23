package org.mcphackers.launchwrapper.tweak.injection.legacy;

import static org.mcphackers.launchwrapper.util.asm.InsnHelper.*;
import static org.objectweb.asm.Opcodes.*;

import org.mcphackers.launchwrapper.LaunchConfig;
import org.mcphackers.launchwrapper.tweak.injection.InjectionWithContext;
import org.mcphackers.launchwrapper.util.ClassNodeSource;
import org.mcphackers.launchwrapper.util.asm.IdentifyCall;
import org.mcphackers.launchwrapper.util.asm.NodeHelper;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
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
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

public class AddMain extends InjectionWithContext<LegacyTweakContext> {

	public AddMain(LegacyTweakContext storage) {
		super(storage);
	}

	public String name() {
		return "Add main";
	}

	public boolean required() {
		return true;
	}

	public boolean apply(ClassNodeSource source, LaunchConfig config) {
		patchMinecraftInit(config);

		ClassNode minecraft = context.getMinecraft();
		MethodNode main = NodeHelper.getMethod(minecraft, "main", "([Ljava/lang/String;)V");

		if (main == null) {
			minecraft.methods.add(getMain(source, config));
		} else {
			minecraft.methods.remove(main);
			minecraft.methods.add(getMain(source, config));
		}
		return true;
	}

	private void patchMinecraftInit(LaunchConfig config) {
		ClassNode minecraft = context.getMinecraft();
		for (MethodNode m : minecraft.methods) {
			if (m.name.equals("<init>")) {
				InsnList insert = new InsnList();
				boolean widthReplaced = false;
				boolean heightReplaced = false;
				boolean fullscreenReplaced = false;
				for (AbstractInsnNode insn : iterator(m.instructions)) {
					AbstractInsnNode[] insns = fill(insn, 3);
					if (context.fullscreenField != null &&
						compareInsn(insns[0], ALOAD) &&
						compareInsn(insns[2], PUTFIELD, minecraft.name, context.fullscreenField.name, context.fullscreenField.desc)) {
						m.instructions.set(insns[1], booleanInsn(config.fullscreen.get()));
					}
					if (context.width != null && context.height != null) {
						if (compareInsn(insns[0], ALOAD) &&
							compareInsn(insns[2], PUTFIELD, minecraft.name, context.width.name, context.width.desc)) {
							m.instructions.set(insns[1], intInsn(config.width.get()));
							widthReplaced = true;
						} else if (compareInsn(insns[0], ALOAD) &&
								   compareInsn(insns[2], PUTFIELD, minecraft.name, context.height.name, context.height.desc)) {
							m.instructions.set(insns[1], intInsn(config.height.get()));
							heightReplaced = true;
						}
					}
				}
				int thisIndex = 0;
				if (context.width != null && context.height != null) {
					if (!widthReplaced) {
						insert.add(new VarInsnNode(ALOAD, thisIndex));
						insert.add(intInsn(config.width.get()));
						insert.add(new FieldInsnNode(PUTFIELD, minecraft.name, context.width.name, context.width.desc));
					}
					if (!heightReplaced) {
						insert.add(new VarInsnNode(ALOAD, thisIndex));
						insert.add(intInsn(config.height.get()));
						insert.add(new FieldInsnNode(PUTFIELD, minecraft.name, context.height.name, context.height.desc));
					}
				}
				if (!fullscreenReplaced && context.fullscreenField != null) {
					insert.add(new VarInsnNode(ALOAD, thisIndex));
					insert.add(booleanInsn(config.fullscreen.get()));
					insert.add(new FieldInsnNode(PUTFIELD, minecraft.name, context.fullscreenField.name, context.fullscreenField.desc));
				}
				if (context.defaultWidth != null && context.defaultHeight != null) {
					insert.add(new VarInsnNode(ALOAD, thisIndex));
					insert.add(intInsn(config.width.get()));
					insert.add(new FieldInsnNode(PUTFIELD, minecraft.name, context.defaultWidth.name, context.defaultWidth.desc));
					insert.add(new VarInsnNode(ALOAD, thisIndex));
					insert.add(intInsn(config.height.get()));
					insert.add(new FieldInsnNode(PUTFIELD, minecraft.name, context.defaultHeight.name, context.defaultHeight.desc));
				}
				m.instructions.insert(getSuper(getFirst(m.instructions)), insert);
			}
		}
	}

	protected MethodNode getMain(ClassNodeSource source, LaunchConfig config) {
		ClassNode minecraft = context.getMinecraft();
		ClassNode minecraftApplet = context.getMinecraftApplet();
		MethodNode node = new MethodNode(ACC_PUBLIC | ACC_STATIC, "main", "([Ljava/lang/String;)V", null, null);
		InsnList insns = node.instructions;

		final int appletIndex = 1;
		final int frameIndex = 2;
		final int mcIndex = 3;

		final String listenerClass = "org/mcphackers/launchwrapper/inject/WindowListener";

		String mcField = null;
		String mcDesc = "L" + minecraft.name + ";";
		boolean invokeAppletInit = patchAppletInit(source, config);
		if (invokeAppletInit) {
			for (FieldNode field : minecraftApplet.fields) {
				if (mcDesc.equals(field.desc)) {
					mcField = field.name;
				}
			}
			insns.add(new TypeInsnNode(NEW, minecraftApplet.name));
			insns.add(new InsnNode(DUP));
			insns.add(new MethodInsnNode(INVOKESPECIAL, minecraftApplet.name, "<init>", "()V"));
			insns.add(new VarInsnNode(ASTORE, appletIndex));
			insns.add(new VarInsnNode(ALOAD, appletIndex));
			insns.add(new MethodInsnNode(INVOKESTATIC, "org/mcphackers/launchwrapper/inject/Inject", "getApplet", "()Lorg/mcphackers/launchwrapper/tweak/AppletWrapper;"));
			insns.add(new MethodInsnNode(INVOKEVIRTUAL, "java/applet/Applet", "setStub", "(Ljava/applet/AppletStub;)V"));
			insns.add(new VarInsnNode(ALOAD, appletIndex));
			insns.add(new MethodInsnNode(INVOKEVIRTUAL, minecraftApplet.name, "init", "()V"));
		}
		if (!config.lwjglFrame.get()) {
			insns.add(new TypeInsnNode(NEW, "java/awt/Frame"));
			insns.add(new InsnNode(DUP));
			insns.add(new LdcInsnNode(config.title.get() == null ? "Minecraft" : config.title.get()));
			insns.add(new MethodInsnNode(INVOKESPECIAL, "java/awt/Frame", "<init>", "(Ljava/lang/String;)V"));
			insns.add(new VarInsnNode(ASTORE, frameIndex));
			insns.add(new VarInsnNode(ALOAD, frameIndex));
			insns.add(booleanInsn(context.isClassic()));
			insns.add(new MethodInsnNode(INVOKESTATIC, "org/mcphackers/launchwrapper/util/IconUtils", "getIcon", "(Z)Ljava/awt/image/BufferedImage;"));
			insns.add(new MethodInsnNode(INVOKEVIRTUAL, "java/awt/Frame", "setIconImage", "(Ljava/awt/Image;)V"));
			insns.add(new VarInsnNode(ALOAD, frameIndex));
			insns.add(new TypeInsnNode(NEW, "java/awt/BorderLayout"));
			insns.add(new InsnNode(DUP));
			insns.add(new MethodInsnNode(INVOKESPECIAL, "java/awt/BorderLayout", "<init>", "()V"));
			insns.add(new MethodInsnNode(INVOKEVIRTUAL, "java/awt/Frame", "setLayout", "(Ljava/awt/LayoutManager;)V"));
			insns.add(new VarInsnNode(ALOAD, frameIndex));
			insns.add(new VarInsnNode(ALOAD, appletIndex));
			insns.add(new LdcInsnNode("Center"));
			insns.add(new MethodInsnNode(INVOKEVIRTUAL, "java/awt/Frame", "add", "(Ljava/awt/Component;Ljava/lang/Object;)V"));
			insns.add(new VarInsnNode(ALOAD, frameIndex));
			insns.add(new MethodInsnNode(INVOKEVIRTUAL, "java/awt/Frame", "pack", "()V"));
			insns.add(new VarInsnNode(ALOAD, frameIndex));
			insns.add(new InsnNode(ACONST_NULL));
			insns.add(new MethodInsnNode(INVOKEVIRTUAL, "java/awt/Frame", "setLocationRelativeTo", "(Ljava/awt/Component;)V"));
		}
		if (invokeAppletInit) {
			insns.add(new VarInsnNode(ALOAD, appletIndex));
			insns.add(new FieldInsnNode(GETFIELD, minecraftApplet.name, mcField, mcDesc));
		} else {
			insns.add(getNewMinecraftImpl(minecraft, null, config));
		}
		insns.add(new VarInsnNode(ASTORE, mcIndex));
		if (context.width != null && context.height != null) {
			insns.add(new VarInsnNode(ALOAD, mcIndex));
			insns.add(intInsn(config.width.get()));
			insns.add(new FieldInsnNode(PUTFIELD, minecraft.name, context.width.name, context.width.desc));
			insns.add(new VarInsnNode(ALOAD, mcIndex));
			insns.add(intInsn(config.height.get()));
			insns.add(new FieldInsnNode(PUTFIELD, minecraft.name, context.height.name, context.height.desc));
		}
		if (context.fullscreenField != null) {
			insns.add(new VarInsnNode(ALOAD, mcIndex));
			insns.add(booleanInsn(config.fullscreen.get()));
			insns.add(new FieldInsnNode(PUTFIELD, minecraft.name, context.fullscreenField.name, context.fullscreenField.desc));
		}
		if (context.defaultWidth != null && context.defaultHeight != null) {
			insns.add(new VarInsnNode(ALOAD, mcIndex));
			insns.add(intInsn(config.width.get()));
			insns.add(new FieldInsnNode(PUTFIELD, minecraft.name, context.defaultWidth.name, context.defaultWidth.desc));
			insns.add(new VarInsnNode(ALOAD, mcIndex));
			insns.add(intInsn(config.height.get()));
			insns.add(new FieldInsnNode(PUTFIELD, minecraft.name, context.defaultHeight.name, context.defaultHeight.desc));
		}
		if (context.appletMode != null) {
			insns.add(new VarInsnNode(ALOAD, mcIndex));
			insns.add(booleanInsn(config.applet.get()));
			insns.add(new FieldInsnNode(PUTFIELD, minecraft.name, context.appletMode.name, context.appletMode.desc));
		}
		if (config.lwjglFrame.get()) {
			insns.add(new TypeInsnNode(NEW, "java/lang/Thread"));
			insns.add(new InsnNode(DUP));
			insns.add(new VarInsnNode(ALOAD, mcIndex));
			insns.add(new LdcInsnNode("Minecraft main thread"));
			insns.add(new MethodInsnNode(INVOKESPECIAL, "java/lang/Thread", "<init>", "(Ljava/lang/Runnable;Ljava/lang/String;)V"));
			insns.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/Thread", "start", "()V"));
			// insns.add(new VarInsnNode(ALOAD, mcIndex));
			// insns.add(new MethodInsnNode(INVOKEINTERFACE, "java/lang/Runnable", "run", "()V"));
		} else {
			insns.add(new VarInsnNode(ALOAD, frameIndex));
			insns.add(new InsnNode(ICONST_1));
			insns.add(new MethodInsnNode(INVOKEVIRTUAL, "java/awt/Frame", "setVisible", "(Z)V"));
			insns.add(new VarInsnNode(ALOAD, frameIndex));
			insns.add(new TypeInsnNode(NEW, listenerClass));
			insns.add(new InsnNode(DUP));
			insns.add(new VarInsnNode(ALOAD, mcIndex));
			insns.add(new MethodInsnNode(INVOKESPECIAL, listenerClass, "<init>", "(L" + minecraft.name + ";)V"));
			insns.add(new MethodInsnNode(INVOKEVIRTUAL, "java/awt/Frame", "addWindowListener", "(Ljava/awt/event/WindowListener;)V"));
			createWindowListener(source, listenerClass);
		}
		insns.add(new InsnNode(RETURN));
		return node;
	}

	protected int applyAccess(int access, int targetAccess) {
		// ACC_PRIVATE, ACC_PROTECTED and ACC_PUBLIC are the first 3 bits
		return (access & (~0x7)) | targetAccess;
	}

	protected boolean patchAppletInit(ClassNodeSource source, LaunchConfig config) {
		ClassNode minecraft = context.getMinecraft();
		ClassNode minecraftApplet = context.getMinecraftApplet();
		if (minecraftApplet == null) {
			return false;
		}
		String mcField = null;
		String canvasField = null;
		String mcDesc = "L" + minecraft.name + ";";
		MethodNode init = NodeHelper.getMethod(minecraftApplet, "init", "()V");
		if (init == null) {
			return false;
		}
		for (FieldNode field : minecraftApplet.fields) {
			if (mcDesc.equals(field.desc)) {
				field.access = applyAccess(field.access, Opcodes.ACC_PUBLIC);
				mcField = field.name;
			}
			if ("Ljava/awt/Canvas;".equals(field.desc)) {
				canvasField = field.name;
			}
		}
		AbstractInsnNode insn = getFirst(init.instructions);
		while (insn != null) {
			AbstractInsnNode[] insns2 = fill(insn, 6);
			if (compareInsn(insns2[1], PUTFIELD, minecraftApplet.name, mcField, mcDesc) &&
				compareInsn(insns2[0], INVOKESPECIAL, null, "<init>")) {
				MethodInsnNode invoke = (MethodInsnNode)insns2[0];
				InsnList constructor = getNewMinecraftImpl(source.getClass(invoke.owner), canvasField, config);
				if (constructor != null) {
					InsnList insns = new InsnList();
					insns.add(new VarInsnNode(ALOAD, 0));
					insns.add(new FieldInsnNode(GETFIELD, minecraftApplet.name, canvasField, "Ljava/awt/Canvas;"));
					insns.add(new TypeInsnNode(NEW, "java/awt/Dimension"));
					insns.add(new InsnNode(DUP));
					insns.add(intInsn(config.width.get()));
					insns.add(intInsn(config.height.get()));
					insns.add(new MethodInsnNode(INVOKESPECIAL, "java/awt/Dimension", "<init>", "(II)V"));
					insns.add(new MethodInsnNode(INVOKEVIRTUAL, "java/awt/Canvas", "setPreferredSize", "(Ljava/awt/Dimension;)V"));
					init.instructions.insert(insns2[1], insns);
					init.instructions.insertBefore(insns2[1], constructor);
					IdentifyCall call = new IdentifyCall(invoke);
					AbstractInsnNode newInsn = call.getArgument(0)[0].getPrevious();
					if (newInsn.getOpcode() == NEW) {
						init.instructions.remove(newInsn);
					}
					for (AbstractInsnNode[] arg : call.getArguments()) {
						remove(init.instructions, arg);
					}
					init.instructions.remove(invoke);
				}
				insn = insns2[1];
			}
			if (compareInsn(insns2[0], ALOAD, 0) &&
				compareInsn(insns2[1], GETFIELD, minecraftApplet.name, mcField, mcDesc) &&
				compareInsn(insns2[2], NEW) &&
				compareInsn(insns2[3], DUP) &&
				compareInsn(insns2[4], INVOKESPECIAL, null, "<init>", "()V") &&
				compareInsn(insns2[5], PUTFIELD, minecraft.name)) {
				TypeInsnNode type = (TypeInsnNode)insns2[2];
				ClassNode node = source.getClass(type.desc);
				MethodNode method = NodeHelper.getMethod(node, "<init>", "()V");
				AbstractInsnNode[] insns3 = fill(getFirst(method.instructions), 4);
				if (compareInsn(insns3[0], ALOAD, 0) &&
					compareInsn(insns3[1], LDC, "DemoUser") &&
					compareInsn(insns3[2], LDC, "n/a") &&
					compareInsn(insns3[3], INVOKESPECIAL, node.superName, "<init>", "(Ljava/lang/String;Ljava/lang/String;)V")) {
					InsnList insert = new InsnList();
					insert.add(new LdcInsnNode(config.username.get()));
					insert.add(new LdcInsnNode(config.session.get()));
					method.instructions.insert(insns2[3], insert);
					method.instructions.set(insns2[2], new TypeInsnNode(NEW, node.superName));
					method.instructions.set(insns2[4], new MethodInsnNode(INVOKESPECIAL, node.superName, "<init>", "(Ljava/lang/String;Ljava/lang/String;)V"));
				}
			}
			if (compareInsn(insns2[0], ALOAD, 0) &&
				compareInsn(insns2[1], GETFIELD, minecraftApplet.name, mcField, mcDesc) &&
				compareInsn(insns2[2], LDC, "79.136.77.240") && // hardcoded IP in c0.0.15a
				compareInsn(insns2[3], SIPUSH) &&
				compareInsn(insns2[4], INVOKEVIRTUAL, minecraft.name, null, "(Ljava/lang/String;I)V")) {
				LabelNode label = new LabelNode();
				init.instructions.remove(insns2[2]);
				init.instructions.remove(insns2[3]);
				InsnList insert = new InsnList();
				insert.add(new VarInsnNode(ALOAD, 0));
				insert.add(new LdcInsnNode("server"));
				insert.add(new MethodInsnNode(INVOKEVIRTUAL, minecraftApplet.name, "getParameter", "(Ljava/lang/String;)Ljava/lang/String;"));
				insert.add(new VarInsnNode(ALOAD, 0));
				insert.add(new LdcInsnNode("port"));
				insert.add(new MethodInsnNode(INVOKEVIRTUAL, minecraftApplet.name, "getParameter", "(Ljava/lang/String;)Ljava/lang/String;"));
				insert.add(new MethodInsnNode(INVOKESTATIC, "java/lang/Integer", "parseInt", "(Ljava/lang/String;)I"));
				init.instructions.insertBefore(insns2[4], insert);
				insert = new InsnList();
				insert.add(new VarInsnNode(ALOAD, 0));
				insert.add(new LdcInsnNode("server"));
				insert.add(new MethodInsnNode(INVOKEVIRTUAL, minecraftApplet.name, "getParameter", "(Ljava/lang/String;)Ljava/lang/String;"));
				insert.add(new JumpInsnNode(IFNULL, label));
				insert.add(new VarInsnNode(ALOAD, 0));
				insert.add(new LdcInsnNode("port"));
				insert.add(new MethodInsnNode(INVOKEVIRTUAL, minecraftApplet.name, "getParameter", "(Ljava/lang/String;)Ljava/lang/String;"));
				insert.add(new JumpInsnNode(IFNULL, label));
				init.instructions.insertBefore(insns2[0], insert);
				init.instructions.insert(insns2[4], label);
			}
			insn = nextInsn(insn);
		}
		source.overrideClass(minecraftApplet);
		return true;
	}

	private void createWindowListener(ClassNodeSource source, String listenerClass) {
		ClassNode minecraft = context.getMinecraft();
		FieldNode running = context.getIsRunning();
		running.access = applyAccess(running.access, Opcodes.ACC_PUBLIC);

		ClassNode node = new ClassNode();
		node.visit(49, ACC_PUBLIC, listenerClass, null, "java/awt/event/WindowAdapter", null);
		node.fields.add(new FieldNode(ACC_PRIVATE, "mc", "L" + minecraft.name + ";", null, null));
		MethodNode init = new MethodNode(ACC_PUBLIC, "<init>", "(L" + minecraft.name + ";)V", null, null);
		InsnList insns = init.instructions;
		insns.add(new VarInsnNode(ALOAD, 0));
		insns.add(new VarInsnNode(ALOAD, 1));
		insns.add(new FieldInsnNode(PUTFIELD, listenerClass, "mc", "L" + minecraft.name + ";"));
		insns.add(new VarInsnNode(ALOAD, 0));
		insns.add(new MethodInsnNode(INVOKESPECIAL, "java/awt/event/WindowAdapter", "<init>", "()V"));
		insns.add(new InsnNode(RETURN));
		node.methods.add(init);

		MethodNode windowClosing = new MethodNode(ACC_PUBLIC, "windowClosing", "(Ljava/awt/event/WindowEvent;)V", null, null);
		insns = windowClosing.instructions;

		insns.add(new VarInsnNode(ALOAD, 0));
		insns.add(new FieldInsnNode(GETFIELD, listenerClass, "mc", "L" + minecraft.name + ";"));
		insns.add(new InsnNode(ICONST_0));
		insns.add(new FieldInsnNode(PUTFIELD, minecraft.name, running.name, running.desc));
		insns.add(new InsnNode(RETURN));
		node.methods.add(windowClosing);
		source.overrideClass(node);
	}

	private InsnList getNewMinecraftImpl(ClassNode minecraftImpl, String canvasField, LaunchConfig config) {
		ClassNode minecraftApplet = context.getMinecraftApplet();
		MethodNode init = null;
		for (MethodNode m : minecraftImpl.methods) {
			if (m.name.equals("<init>")) {
				init = m;
			}
		}
		if (init == null) {
			throw new NullPointerException();
		}

		InsnList insns = new InsnList();
		insns.add(new TypeInsnNode(NEW, minecraftImpl.name));
		insns.add(new InsnNode(DUP));

		Type[] types = Type.getArgumentTypes(init.desc);

		int i = 0;
		for (Type type : types) {
			String desc = type.getDescriptor();
			if (desc.equals("Ljava/awt/Canvas;")) {
				if (config.lwjglFrame.get()) {
					insns.add(new InsnNode(ACONST_NULL));
				} else {
					insns.add(new VarInsnNode(ALOAD, 0));
					insns.add(new FieldInsnNode(GETFIELD, minecraftApplet.name, canvasField, "Ljava/awt/Canvas;"));
				}
			} else if (desc.equals("Ljava/awt/Component;")) {
				if (config.lwjglFrame.get()) {
					insns.add(new InsnNode(ACONST_NULL));
				} else {
					insns.add(new VarInsnNode(ALOAD, 0));
				}
			} else if (minecraftApplet != null && desc.equals("L" + minecraftApplet.name + ";")) {
				insns.add(new VarInsnNode(ALOAD, 0));
			} else if (desc.equals("I")) {
				if (i == 0) {
					insns.add(intInsn(config.width.get()));
				} else if (i == 1) {
					insns.add(intInsn(config.height.get()));
				} else {
					throw new IllegalArgumentException("Unexpected Minecraft constructor: " + init.desc);
				}
				i++;
			} else if (desc.equals("Z")) {
				insns.add(booleanInsn(config.fullscreen.get()));
			} else {
				throw new IllegalArgumentException("Unexpected Minecraft constructor: " + init.desc);
			}
		}
		insns.add(new MethodInsnNode(INVOKESPECIAL, minecraftImpl.name, "<init>", init.desc));
		return insns;
	}
}
