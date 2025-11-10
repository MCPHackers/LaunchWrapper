package org.mcphackers.launchwrapper.tweak.injection.legacy;

import static org.mcphackers.launchwrapper.util.asm.InsnHelper.*;
import static org.objectweb.asm.Opcodes.*;

import org.mcphackers.launchwrapper.LaunchConfig;
import org.mcphackers.launchwrapper.tweak.injection.InjectionWithContext;
import org.mcphackers.launchwrapper.util.ClassNodeSource;
import org.mcphackers.launchwrapper.util.asm.NodeHelper;
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

/**
 * Includes deAWT, patching of title and icon, initial resolution and etc
 */
public class LWJGLPatch extends InjectionWithContext<LegacyTweakContext> {
	private MethodNode tick;

	public LWJGLPatch(LegacyTweakContext storage) {
		super(storage);
	}

	public String name() {
		return "LWJGL Patch";
	}

	public boolean required() {
		return false;
	}

	public boolean apply(ClassNodeSource source, LaunchConfig config) {
		tick = getTickMethod(context.getRun());
		removeCanvas(tick);
		return displayPatch(context.getInit(), context.supportsResizing, source, config);
	}

	private void removeCanvas(MethodNode method) {
		ClassNode minecraft = context.getMinecraft();
		AbstractInsnNode insn1 = getFirst(method.instructions);
		while (insn1 != null) {
			AbstractInsnNode[] insns = fill(insn1, 6);
			if (context.width != null && context.height != null &&
				compareInsn(insns[0], ALOAD) &&
				compareInsn(insns[1], ALOAD) &&
				compareInsn(insns[2], GETFIELD, minecraft.name, context.width.name, context.width.desc) &&
				compareInsn(insns[3], ALOAD) &&
				compareInsn(insns[4], GETFIELD, minecraft.name, context.height.name, context.height.desc) &&
				compareInsn(insns[5], INVOKESPECIAL, minecraft.name, null, "(II)V")) {
				context.supportsResizing = true;
			}
			if (compareInsn(insns[0], ALOAD) &&
				compareInsn(insns[1], GETFIELD, minecraft.name, null, "Ljava/awt/Canvas;") &&
				compareInsn(insns[2], INVOKEVIRTUAL, "java/awt/Canvas", "getWidth", "()I")) {
				MethodInsnNode invoke = new MethodInsnNode(INVOKESTATIC, "org/lwjgl/opengl/Display", "getWidth", "()I");
				method.instructions.insert(insns[2], invoke);
				method.instructions.remove(insns[0]);
				method.instructions.remove(insns[1]);
				method.instructions.remove(insns[2]);
				insn1 = invoke;
			}
			if (compareInsn(insns[0], ALOAD) &&
				compareInsn(insns[1], GETFIELD, minecraft.name, null, "Ljava/awt/Canvas;") &&
				compareInsn(insns[2], INVOKEVIRTUAL, "java/awt/Canvas", "getHeight", "()I")) {
				MethodInsnNode invoke = new MethodInsnNode(INVOKESTATIC, "org/lwjgl/opengl/Display", "getHeight", "()I");
				method.instructions.insert(insns[2], invoke);
				method.instructions.remove(insns[0]);
				method.instructions.remove(insns[1]);
				method.instructions.remove(insns[2]);
				insn1 = invoke;
			}

			if (compareInsn(insns[0], ALOAD) &&
				compareInsn(insns[1], GETFIELD, minecraft.name, null, "Ljava/awt/Canvas;") &&
				compareInsn(insns[2], IFNONNULL) &&
				compareInsn(insns[3], INVOKESTATIC, "org/lwjgl/opengl/Display", "isCloseRequested", "()Z") &&
				compareInsn(insns[4], IFEQ)) {
				method.instructions.remove(insns[0]);
				method.instructions.remove(insns[1]);
				method.instructions.remove(insns[2]);
				insn1 = insns[3];
			}
			insn1 = nextInsn(insn1);
		}
		insn1 = getFirst(method.instructions);
		while (insn1 != null) {
			AbstractInsnNode[] insns = fill(insn1, 6);
			if (compareInsn(insns[0], ALOAD) &&
				compareInsn(insns[1], GETFIELD, minecraft.name, null, "Ljava/awt/Canvas;") &&
				compareInsn(insns[2], IFNULL) &&
				compareInsn(insns[3], ALOAD) &&
				compareInsn(insns[4], GETFIELD, minecraft.name, null, "Z") &&
				compareInsn(insns[5], IFNE)) {
				if (((JumpInsnNode)insns[2]).label != ((JumpInsnNode)insns[5]).label) {
					continue;
				}
				method.instructions.remove(insns[0]);
				method.instructions.remove(insns[1]);
				method.instructions.remove(insns[2]);
				break;
			}
			insn1 = nextInsn(insn1);
		}
	}

	private MethodNode getTickMethod(MethodNode run) {
		ClassNode minecraft = context.getMinecraft();
		FieldNode running = context.getIsRunning();
		if (running == null) {
			// Pre-classic/RubyDung is missing running boolean
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

	private InsnList getIcon() {
		boolean grassIcon = context.isClassic();
		InsnList insert = new InsnList();
		insert.add(booleanInsn(grassIcon));
		insert.add(new MethodInsnNode(INVOKESTATIC, "org/mcphackers/launchwrapper/util/IconUtils", "loadIcon", "(Z)[Ljava/nio/ByteBuffer;"));
		insert.add(new MethodInsnNode(INVOKESTATIC, "org/lwjgl/opengl/Display", "setIcon", "([Ljava/nio/ByteBuffer;)I"));
		insert.add(new InsnNode(POP));
		return insert;
	}

	private boolean displayPatch(MethodNode init, boolean supportsResizing, ClassNodeSource source, LaunchConfig config) {
		ClassNode minecraft = context.getMinecraft();
		boolean foundTitle = false;
		boolean success = false;
		String canvasName = null;

		int thisIndex = 0;
		LabelNode aLabel = new LabelNode();
		LabelNode iLabel = null;
		LabelNode oLabel = null;

		AbstractInsnNode afterLabel = null;

		JumpInsnNode ifNoCanvas = null;
		JumpInsnNode ifFullscreen = null;

		InsnList insnList = init.instructions;
		AbstractInsnNode insn = getFirst(insnList);
		while (insn != null) {
			AbstractInsnNode[] insns = fill(insn, 6);
			if (iLabel == null &&
				compareInsn(insns[0], ALOAD) &&
				compareInsn(insns[1], GETFIELD, minecraft.name, null, "Ljava/awt/Canvas;") &&
				compareInsn(insns[2], IFNULL)) {
				thisIndex = ((VarInsnNode)insns[0]).var;
				canvasName = ((FieldInsnNode)insns[1]).name;
				ifNoCanvas = (JumpInsnNode)insns[2];
				iLabel = ifNoCanvas.label;
				afterLabel = insns[0];
				insn = insns[2];
			}
			if (iLabel == null &&
				compareInsn(insns[0], ALOAD) &&
				compareInsn(insns[1], DUP) &&
				compareInsn(insns[2], ASTORE) &&
				compareInsn(insns[3], GETFIELD, minecraft.name, null, "Ljava/awt/Canvas;") &&
				compareInsn(insns[4], IFNULL)) {
				// Required for in-20100131-2. Has weird bytecode to ASTORE this instance to another var
				thisIndex = ((VarInsnNode)insns[2]).var;
				canvasName = ((FieldInsnNode)insns[3]).name;
				ifNoCanvas = (JumpInsnNode)insns[4];
				iLabel = ifNoCanvas.label;
				VarInsnNode aload = new VarInsnNode(ALOAD, thisIndex);
				insnList.insert(insns[2], aload);
				insnList.remove(insns[1]);
				afterLabel = aload;
				insn = insns[4];
			}

			// Any other pre-classic version
			if (compareInsn(insns[0], NEW, "org/lwjgl/opengl/DisplayMode") &&
				compareInsn(insns[1], DUP) &&
				compareInsn(insns[2], SIPUSH) &&
				compareInsn(insns[3], SIPUSH) &&
				compareInsn(insns[4], INVOKESPECIAL, "org/lwjgl/opengl/DisplayMode", "<init>", "(II)V") &&
				compareInsn(insns[5], INVOKESTATIC, "org/lwjgl/opengl/Display", "setDisplayMode", "(Lorg/lwjgl/opengl/DisplayMode;)V")) {
				InsnList insert = getIcon();
				if (config.vsync.get()) {
					insert.add(new InsnNode(ICONST_1));
					insert.add(new MethodInsnNode(INVOKESTATIC, "org/lwjgl/opengl/Display", "setVSyncEnabled", "(Z)V"));
				}
				if (!config.fullscreen.get()) {
					insnList.set(insns[2], intInsn(config.width.get()));
					insnList.set(insns[3], intInsn(config.height.get()));
					insnList.insert(insns[5], insert);
				} else {
					insert.add(new InsnNode(ICONST_1));
					insert.add(new MethodInsnNode(INVOKESTATIC, "org/lwjgl/opengl/Display", "setFullscreen", "(Z)V"));
					insnList.insert(insns[5], insert);
					removeRange(insnList, insns[0], insns[5]);
				}
			} else if (compareInsn(insns[0], ICONST_1) &&
					   compareInsn(insns[1], INVOKESTATIC, "org/lwjgl/opengl/Display", "setFullscreen", "(Z)V") &&
					   compareInsn(insns[2], INVOKESTATIC, "org/lwjgl/opengl/Display", "create", "()V")) {
				// rd-152252 / rd-160052
				InsnList insert = getIcon();
				if (config.vsync.get()) {
					insert.add(new InsnNode(ICONST_1));
					insert.add(new MethodInsnNode(INVOKESTATIC, "org/lwjgl/opengl/Display", "setVSyncEnabled", "(Z)V"));
				}
				if (!config.fullscreen.get()) {
					insert.add(new TypeInsnNode(NEW, "org/lwjgl/opengl/DisplayMode"));
					insert.add(new InsnNode(DUP));
					insert.add(intInsn(config.width.get()));
					insert.add(intInsn(config.height.get()));
					insert.add(new MethodInsnNode(INVOKESPECIAL, "org/lwjgl/opengl/DisplayMode", "<init>", "(II)V"));
					insert.add(new MethodInsnNode(INVOKESTATIC, "org/lwjgl/opengl/Display", "setDisplayMode", "(Lorg/lwjgl/opengl/DisplayMode;)V"));
					insnList.insert(insns[1], insert);
					insnList.remove(insns[0]);
					insnList.remove(insns[1]);
				} else {
					insnList.insert(insns[1], insert);
				}
			}

			if (oLabel == null &&
				compareInsn(insns[0], ICONST_1) &&
				compareInsn(insns[1], INVOKESTATIC, "org/lwjgl/opengl/Display", "setFullscreen", "(Z)V")) {
				AbstractInsnNode insn2 = insns[0];
				while (insn2 != null) {
					if (insn2.getOpcode() == IFEQ) {
						if (compareInsn(insn2.getPrevious(), GETFIELD)) {
							context.fullscreenField = (FieldInsnNode)insn2.getPrevious();
						}
						ifFullscreen = (JumpInsnNode)insn2;
						oLabel = ifFullscreen.label;
					}
					insn2 = previousInsn(insn2);
				}
			}
			if (compareInsn(insns[0], LDC) &&
				compareInsn(insns[1], INVOKESTATIC, "org/lwjgl/opengl/Display", "setTitle", "(Ljava/lang/String;)V")) {
				LdcInsnNode ldc = (LdcInsnNode)insn;
				if (ldc.cst instanceof String) {
					foundTitle = true;
					String value = (String)ldc.cst;
					if (config.title.get() != null) {
						ldc.cst = config.title.get();
					} else if (value.startsWith("Minecraft Minecraft")) {
						ldc.cst = value.substring(10);
					}
				}
			}
			if (compareInsn(insns[0], GETFIELD, minecraft.name, context.width.name, context.width.desc) &&
				compareInsn(insns[2], GETFIELD, minecraft.name, context.height.name, context.height.desc) &&
				compareInsn(insns[3], INVOKESPECIAL, null, "<init>", "(L" + minecraft.name + ";II)V")) {
				// new Hud(Minecraft, width, height)
				MethodInsnNode invoke = (MethodInsnNode)insns[3];
				ClassNode hud = source.getClass(invoke.owner);
				MethodNode initHud = NodeHelper.getMethod(hud, invoke.name, invoke.desc);

				FieldInsnNode putFieldWidth = null, putFieldHeight = null;
				AbstractInsnNode insn2 = getFirst(initHud.instructions);
				while (insn2 != null) {
					AbstractInsnNode[] insns2 = fill(insn2, 6);

					if (compareInsn(insns2[0], ILOAD) &&
						intValue(insns2[1]) == 240 &&
						compareInsn(insns2[2], IMUL) &&
						compareInsn(insns2[3], ILOAD, 3) &&
						compareInsn(insns2[4], IDIV) &&
						compareInsn(insns2[5], PUTFIELD, hud.name)) {
						VarInsnNode varLoad = (VarInsnNode)insns2[0];
						if (varLoad.var == 2) {
							putFieldWidth = (FieldInsnNode)insns2[5];
						}
						if (varLoad.var == 3) {
							putFieldHeight = (FieldInsnNode)insns2[5];
							break;
						}
					}
					insn2 = nextInsn(insn2);
				}
				for (MethodNode m : hud.methods) {
					if (putFieldWidth == null || putFieldHeight == null) {
						break;
					}
					// render(FZII)V || render(FII)V || render()V
					if (m.desc.equals("(FZII)V") || m.desc.equals("(ZII)V") || m.desc.equals("()V")) {
						insn2 = getFirst(m.instructions);
						while (insn2 != null) {
							if (compareInsn(insn2, GETFIELD, hud.name, null, "L" + minecraft.name + ";")) {
								// Update hud width/heigth values on resize in classic and indev
								InsnList insert = new InsnList();
								insert.add(new InsnNode(DUP)); // MC, MC
								insert.add(new FieldInsnNode(GETFIELD, minecraft.name, context.width.name, context.width.desc));
								insert.add(intInsn(240));																		   // MC, INT, INT
								insert.add(new InsnNode(IMUL));																	   // MC, INT
								insert.add(new InsnNode(SWAP));																	   // INT, MC
								insert.add(new InsnNode(DUP_X1));																   // MC, INT, MC
								insert.add(new FieldInsnNode(GETFIELD, minecraft.name, context.height.name, context.height.desc)); // MC, INT (width * 240), INT (height)
								insert.add(new InsnNode(IDIV));																	   // MC, INT
								insert.add(new VarInsnNode(ALOAD, 0));
								insert.add(new InsnNode(SWAP));
								insert.add(new FieldInsnNode(PUTFIELD, hud.name, putFieldWidth.name, putFieldWidth.desc));

								insert.add(new InsnNode(DUP));
								insert.add(new FieldInsnNode(GETFIELD, minecraft.name, context.height.name, context.height.desc));
								insert.add(intInsn(240));
								insert.add(new InsnNode(IMUL));
								insert.add(new InsnNode(SWAP));
								insert.add(new InsnNode(DUP_X1));
								insert.add(new FieldInsnNode(GETFIELD, minecraft.name, context.height.name, context.height.desc));
								insert.add(new InsnNode(IDIV));
								insert.add(new VarInsnNode(ALOAD, 0));
								insert.add(new InsnNode(SWAP));
								insert.add(new FieldInsnNode(PUTFIELD, hud.name, putFieldHeight.name, putFieldHeight.desc));

								m.instructions.insert(insn2, insert);
								source.overrideClass(hud);
								break;
							}
							insn2 = nextInsn(insn2);
						}
					}
				}
			}

			insn = nextInsn(insn);
		}

		if (afterLabel != null && iLabel != null && oLabel != null && ifNoCanvas != null && ifFullscreen != null) {
			insnList.insertBefore(afterLabel, aLabel);
			InsnList insert = getIcon();
			insert.add(new InsnNode(ICONST_1));
			insert.add(new MethodInsnNode(INVOKESTATIC, "org/lwjgl/opengl/Display", "setResizable", "(Z)V"));
			insert.add(booleanInsn(config.vsync.get()));
			insert.add(new MethodInsnNode(INVOKESTATIC, "org/lwjgl/opengl/Display", "setVSyncEnabled", "(Z)V"));
			insert.add(new VarInsnNode(ALOAD, thisIndex));
			insert.add(new FieldInsnNode(GETFIELD, minecraft.name, canvasName, "Ljava/awt/Canvas;"));
			insert.add(new JumpInsnNode(IFNULL, iLabel));
			insert.add(new VarInsnNode(ALOAD, thisIndex));
			insert.add(new FieldInsnNode(GETFIELD, minecraft.name, canvasName, "Ljava/awt/Canvas;"));
			insert.add(new MethodInsnNode(INVOKESTATIC, "org/lwjgl/opengl/Display", "setParent", "(Ljava/awt/Canvas;)V"));

			insert.add(new JumpInsnNode(GOTO, iLabel));
			insnList.insertBefore(aLabel, insert);
			ifNoCanvas.label = oLabel;
			ifFullscreen.label = aLabel;
			success = true;
		}

		// there is no setTitle call in Pre-classic/RubyDung
		if (!foundTitle && config.title.get() != null) {
			InsnList insert = new InsnList();
			insert.add(new LdcInsnNode(config.title.get()));
			insert.add(new MethodInsnNode(INVOKESTATIC, "org/lwjgl/opengl/Display", "setTitle", "(Ljava/lang/String;)V"));
			insnList.insertBefore(getFirst(insnList), insert);
		}

		if (context.fullscreenField != null) {
			MethodNode toggleFullscreen = null;
		methodLoop:
			for (MethodNode m : minecraft.methods) {
				AbstractInsnNode insn2 = getFirst(m.instructions);
				while (insn2 != null) {
					if (insn2.getOpcode() == GETFIELD) {
						if (compareInsn(insn2, GETFIELD, minecraft.name, context.fullscreenField.name, context.fullscreenField.desc)) {
							break;
						} else {
							continue methodLoop;
						}
					}
					insn2 = nextInsn(insn2);
				}
				while (insn2 != null) {
					AbstractInsnNode[] insns2 = fill(insn2, 4);
					if (compareInsn(insns2[0], ALOAD) &&
						compareInsn(insns2[1], ALOAD) &&
						compareInsn(insns2[2], GETFIELD, minecraft.name, null, context.width.desc) &&
						compareInsn(insns2[3], PUTFIELD, minecraft.name, context.width.name, context.width.desc)) {
						context.defaultWidth = (FieldInsnNode)insns2[2];
					}
					if (compareInsn(insns2[0], ALOAD) &&
						compareInsn(insns2[1], ALOAD) &&
						compareInsn(insns2[2], GETFIELD, minecraft.name, null, context.height.desc) &&
						compareInsn(insns2[3], PUTFIELD, minecraft.name, context.height.name, context.height.desc)) {
						context.defaultHeight = (FieldInsnNode)insns2[2];
					}
					if (context.defaultWidth != null && context.defaultHeight != null && config.lwjglFrame.get() &&
						compareInsn(insns2[0], IFGT) &&
						compareInsn(insns2[1], ALOAD) &&
						compareInsn(insns2[2], ICONST_1) &&
						compareInsn(insns2[3], PUTFIELD, minecraft.name, context.height.name, context.height.desc)) {
						AbstractInsnNode next = nextInsn(insns2[3]);
						AbstractInsnNode[] insns3 = fill(next, 8);
						if (compareInsn(insns3[0], NEW, "org/lwjgl/opengl/DisplayMode") &&
							compareInsn(insns3[1], DUP) &&
							compareInsn(insns3[2], ALOAD) &&
							compareInsn(insns3[3], GETFIELD, minecraft.name, null, context.defaultWidth.desc) &&
							compareInsn(insns3[4], ALOAD) &&
							compareInsn(insns3[5], GETFIELD, minecraft.name, null, context.defaultHeight.desc) &&
							compareInsn(insns3[6], INVOKESPECIAL, "org/lwjgl/opengl/DisplayMode", "<init>", "(II)V") &&
							compareInsn(insns3[7], INVOKESTATIC, "org/lwjgl/opengl/Display", "setDisplayMode", "(Lorg/lwjgl/opengl/DisplayMode;)V")) {
							m.instructions.set(insns3[3], new FieldInsnNode(GETFIELD, minecraft.name, context.defaultWidth.name, context.defaultWidth.desc));
							m.instructions.set(insns3[5], new FieldInsnNode(GETFIELD, minecraft.name, context.defaultHeight.name, context.defaultHeight.desc));
							break;
						} else {
							JumpInsnNode jump = (JumpInsnNode)insns2[0];
							LabelNode newLabel = new LabelNode();
							jump.label = newLabel;
							InsnList insert = new InsnList();
							insert.add(newLabel);
							insert.add(new TypeInsnNode(NEW, "org/lwjgl/opengl/DisplayMode"));
							insert.add(new InsnNode(DUP));
							insert.add(new VarInsnNode(ALOAD, 0));
							insert.add(new FieldInsnNode(GETFIELD, minecraft.name, context.defaultWidth.name, context.defaultWidth.desc));
							insert.add(new VarInsnNode(ALOAD, 0));
							insert.add(new FieldInsnNode(GETFIELD, minecraft.name, context.defaultHeight.name, context.defaultHeight.desc));
							insert.add(new MethodInsnNode(INVOKESPECIAL, "org/lwjgl/opengl/DisplayMode", "<init>", "(II)V"));
							insert.add(new MethodInsnNode(INVOKESTATIC, "org/lwjgl/opengl/Display", "setDisplayMode", "(Lorg/lwjgl/opengl/DisplayMode;)V"));
							m.instructions.insert(insns2[3], insert);
							break;
						}
					}
					insn2 = nextInsn(insn2);
				}
				insn2 = getFirst(m.instructions);
				while (insn2 != null) {
					AbstractInsnNode[] insns2 = fill(insn2, 4);
					if (compareInsn(insns2[0], INVOKESTATIC, "org/lwjgl/opengl/Display", "setFullscreen", "(Z)V") &&
						compareInsn(insns2[1], INVOKESTATIC, "org/lwjgl/opengl/Display", "update", "()V")) {
						// Removes fullscreen delay
						if (compareInsn(insns2[2], LDC, 1000L) &&
							compareInsn(insns2[3], INVOKESTATIC, "java/lang/Thread", "sleep", "(J)V")) {
							m.instructions.remove(insns2[2]);
							m.instructions.remove(insns2[3]);
						}
						toggleFullscreen = m;
					}

					insn2 = nextInsn(insn2);
				}
			}
			for (AbstractInsnNode insn2 = getFirst(tick.instructions); insn2 != null; insn2 = nextInsn(insn2)) {
				AbstractInsnNode[] insns2 = fill(insn2, 7);
				if (toggleFullscreen != null) {
					if (compareInsn(insns2[0], INVOKESTATIC, "org/lwjgl/opengl/Display", "isActive", "()Z") &&
						compareInsn(insns2[1], IFNE) &&
						compareInsn(insns2[2], ALOAD) &&
						compareInsn(insns2[3], GETFIELD, minecraft.name, context.fullscreenField.name, context.fullscreenField.desc) &&
						compareInsn(insns2[4], IFEQ) &&
						compareInsn(insns2[5], ALOAD) &&
						compareInsn(insns2[6], INVOKEVIRTUAL, minecraft.name, toggleFullscreen.name, toggleFullscreen.desc)) {
						tick.instructions.set(insn2, new InsnNode(ICONST_1));
					}
				}
				if (!supportsResizing) {
					if (compareInsn(insns2[0], INVOKESTATIC, "org/lwjgl/opengl/Display", "isCloseRequested", "()Z") &&
						compareInsn(insns2[1], IFEQ)) {
						JumpInsnNode jmp = (JumpInsnNode)insns2[1];
						InsnList insert = new InsnList();
						insert.add(new VarInsnNode(ALOAD, 0));
						insert.add(new MethodInsnNode(INVOKESTATIC, "org/lwjgl/opengl/Display", "getWidth", "()I"));
						insert.add(new FieldInsnNode(PUTFIELD, minecraft.name, context.width.name, context.width.desc));
						insert.add(new VarInsnNode(ALOAD, 0));
						insert.add(new MethodInsnNode(INVOKESTATIC, "org/lwjgl/opengl/Display", "getHeight", "()I"));
						insert.add(new FieldInsnNode(PUTFIELD, minecraft.name, context.height.name, context.height.desc));
						tick.instructions.insert(jmp.label, insert);
					}
				}
			}
		}

		source.overrideClass(minecraft);
		return success;
	}
}
