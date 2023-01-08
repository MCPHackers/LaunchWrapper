package org.mcphackers.launchwrapper.tweak;

import static org.mcphackers.launchwrapper.inject.InsnHelper.*;
import static org.objectweb.asm.Opcodes.*;

import java.lang.reflect.Field;

import org.mcphackers.launchwrapper.LaunchConfig;
import org.mcphackers.launchwrapper.LaunchTarget;
import org.mcphackers.launchwrapper.MainLaunchTarget;
import org.mcphackers.launchwrapper.inject.ClassNodeSource;
import org.mcphackers.launchwrapper.inject.InjectUtils;
import org.mcphackers.launchwrapper.inject.InjectUtils.Access;
import org.mcphackers.launchwrapper.loader.LaunchClassLoader;
import org.mcphackers.launchwrapper.protocol.LegacyURLStreamHandler;
import org.mcphackers.launchwrapper.protocol.LegacyURLStreamHandler.SkinType;
import org.mcphackers.launchwrapper.protocol.URLStreamHandlerProxy;
import org.mcphackers.launchwrapper.util.Util;
import org.mcphackers.rdi.util.IdentifyCall;
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
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

public class LegacyTweak extends Tweak {
	
	protected LaunchConfig launch;

	public static final String[] MAIN_CLASSES = {
			"net/minecraft/client/Minecraft",
			"com/mojang/minecraft/Minecraft",
			"com/mojang/minecraft/RubyDung",
			"com/mojang/rubydung/RubyDung"
	};
	public static final String[] MAIN_APPLETS = {
			"net/minecraft/client/MinecraftApplet",
			"com/mojang/minecraft/MinecraftApplet"
	};
	
	public static final boolean experimentalIndevSaving = true;

	protected ClassNode minecraft;
	protected ClassNode minecraftApplet;
	/** Field that determines if Minecraft should exit */
	protected FieldNode running;
	/** Frame width */
	protected FieldNode width;
	/** Frame height */
	protected FieldNode height;
	/** Working game directory */
	protected FieldNode mcDir;
	/** Whenever the game runs in an applet */
	private FieldNode appletMode;
	private FieldInsnNode defaultWidth;
	private FieldInsnNode defaultHeight;
	private FieldInsnNode fullscreenField;
	private String mouseHelperName;
	private boolean supportsResizing;
	/** public static main(String[]) */
	protected MethodNode main;
	protected SkinType skinType = SkinType.DEFAULT;
	protected int port = -1;
	
	public LegacyTweak(ClassNodeSource source, LaunchConfig launch) {
		super(source);
		this.launch = launch;
	}

	public boolean transform() {
		init();
		if(launch.skinProxy.get() != null) {
			skinType = SkinType.get(launch.skinProxy.get());
		}
		if(launch.resourcesProxyPort.get() != null) {
			port = launch.resourcesProxyPort.get();
		}
		if(minecraft == null) {
			return false;
		}
    	MethodNode run = InjectUtils.getMethod(minecraft, "run", "()V");
    	if(run == null) {
    		return false;
    	}
    	MethodNode runTick = getTickMethod(run);
		fixSplash();
    	fixPaulscode();
    	fixIndevLaunch();
    	addIndevSaving();
    	fixA111GrayScreen();
    	fixShutdown(run);
    	removeCanvas(runTick);
		MethodNode init = getInit(run);
		replaceGameDirectory(init, mcDir);
		optionsLoadFix(init);
		displayPatch(init, supportsResizing);
		fixMouseHelper(mouseHelperName);
    	patchMinecraftInit();
    	
		if(main == null) {
			minecraft.methods.add(main = getMain());
		} else {
			minecraft.methods.remove(main);
			minecraft.methods.add(main = getMain());
		}
		
		source.overrideClass(minecraft);
		return true;
	}

	public LaunchTarget getLaunchTarget(LaunchClassLoader loader) {
		if(main != null) {
			enableLegacyMergeSort();
			URLStreamHandlerProxy.setURLStreamHandler("http", new LegacyURLStreamHandler(skinType, port));
			MainLaunchTarget target = new MainLaunchTarget(loader, minecraft.name);
			target.args = new String[] {launch.username.get(), launch.sessionid.get()};
			return target;
		}
		return null;
	}

	private void addIndevSaving() {
		if(!experimentalIndevSaving) {
			return;
		}
		ClassNode saveLevelMenu = null;
		ClassNode loadLevelMenu = null;
		methods:
		for(MethodNode m : minecraft.methods) {
			AbstractInsnNode[] insns = fill(m.instructions.getFirst(), 10);
			if(compareInsn(insns[0], ALOAD, 0)
		    && compareInsn(insns[1], GETFIELD, minecraft.name)
		    && compareInsn(insns[2], IFNULL)
		    && compareInsn(insns[3], RETURN)
		    && compareInsn(insns[4], ALOAD, 0)
		    && compareInsn(insns[5], NEW)
		    && compareInsn(insns[6], DUP)
		    && compareInsn(insns[7], INVOKESPECIAL, null, "<init>", "()V")
		    && compareInsn(insns[8], INVOKEVIRTUAL)
		    && compareInsn(insns[9], RETURN)) {
				ClassNode pauseMenu = source.getClass(((TypeInsnNode)insns[5]).desc);
				if(pauseMenu == null) {
					return;
				}
				for(MethodNode m2 : pauseMenu.methods) {
					AbstractInsnNode insn = m2.instructions.getFirst();
					AbstractInsnNode[] insns2 = fill(insn, 3);
					if(compareInsn(insns2[0], ALOAD, 1)
				    && compareInsn(insns2[1], GETFIELD, null, null, "I")
				    && compareInsn(insns2[2], IFNE)) {
						FieldInsnNode idField = (FieldInsnNode)insns2[1];
						while(insn != null) {
							insns2 = fill(insn, 4);
							if(compareInsn(insns2[0], ALOAD, 1)
						    && compareInsn(insns2[1], GETFIELD, idField.owner, idField.name, idField.desc)
						    && compareInsn(insns2[3], IF_ICMPNE)) {
								AbstractInsnNode[] insns3 = fill(nextInsn(insns2[3]), 3);
								if(compareInsn(insns3[2], NEW)) {
									if(compareInsn(insns2[2], ICONST_2)) {
										saveLevelMenu = source.getClass(((TypeInsnNode)insns3[2]).desc);
									} else
									if(compareInsn(insns2[2], ICONST_3)) {
										loadLevelMenu = source.getClass(((TypeInsnNode)insns3[2]).desc);
									}
								}
							}
							if(saveLevelMenu != null && loadLevelMenu != null) {
								break methods;
							}
							insn = nextInsn(insn);
						}
					}
				}
			}
		}
		if(saveLevelMenu == null || loadLevelMenu == null) {
			return;
		}
		ClassNode nameLevelMenu = null;
		MethodNode openFileLoad = null;
		for(MethodNode m : loadLevelMenu.methods) {
			if(openFileLoad == null && m.desc.equals("(Ljava/io/File;)V")) {
				openFileLoad = m;
				break;
			}
		}
		if(openFileLoad == null) {
			return;
		}
		for(MethodNode m : loadLevelMenu.methods) {
			if(m.desc.equals("(I)V")) {
				InsnList insns = new InsnList();
				insns.add(new VarInsnNode(ALOAD, 0));
				insns.add(new VarInsnNode(ILOAD, 1));
				insns.add(new MethodInsnNode(INVOKESTATIC, "org/mcphackers/launchwrapper/inject/Inject", "getLevelFile", "(I)Ljava/io/File;"));
				insns.add(new MethodInsnNode(INVOKEVIRTUAL, loadLevelMenu.name, openFileLoad.name, openFileLoad.desc));
				m.instructions.insert(insns);
				break;
			}
		}
		MethodNode openFile = null;
		for(MethodNode m : saveLevelMenu.methods) {
			if(m.desc.equals("(I)V")) {
				for(AbstractInsnNode insn : m.instructions) {
					if(insn.getOpcode() == NEW) {
						nameLevelMenu = source.getClass(((TypeInsnNode)insn).desc);
						break;
					}
				}
			}
		}
		for(MethodNode m : saveLevelMenu.methods) {
			if(openFile == null && m.desc.equals("(Ljava/io/File;)V")) {
				openFile = new MethodNode(m.access, m.name, m.desc, m.signature, null);
				m.accept(openFile);
				AbstractInsnNode insn = openFile.instructions.getFirst();
				while(insn != null) {
					if(insn.getOpcode() == GETFIELD) {
						FieldInsnNode field = (FieldInsnNode)insn;
						if(compareInsn(insn.getPrevious(), ALOAD, 0)) {
							FieldInsnNode getField = new FieldInsnNode(GETFIELD, nameLevelMenu.name, field.name, field.desc);
							openFile.instructions.set(insn, getField);
							insn = getField;
						}
					}
					insn = nextInsn(insn);
				}
				break;
			}
		}
		if(openFile == null) {
			return;
		}
		FieldInsnNode idField = null;
		for(MethodNode m : nameLevelMenu.methods) {
			Type[] types = Type.getArgumentTypes(m.desc);
			if(m.name.equals("<init>") && types.length > 0 && types[types.length - 1].getDescriptor().equals("I")) {
				AbstractInsnNode insn = m.instructions.getFirst();
				while(insn != null) {
					AbstractInsnNode[] insns = fill(insn, 2);
					if(compareInsn(insns[0], ILOAD, 3)
					&& compareInsn(insns[1], PUTFIELD)) {
						idField = (FieldInsnNode)insns[1];
					}
					insn = nextInsn(insn);
				}
			}
		}
		for(MethodNode m : nameLevelMenu.methods) {
			if(Type.getArgumentTypes(m.desc).length == 1) {
				AbstractInsnNode insn = m.instructions.getFirst();
				while(insn != null) {
					AbstractInsnNode[] insns2 = fill(insn, 4);
					if(compareInsn(insns2[0], ALOAD, 0)
				    && compareInsn(insns2[1], GETFIELD)
				    && compareInsn(insns2[2], INVOKEVIRTUAL, "java/lang/String", "trim", "()Ljava/lang/String;")
				    && compareInsn(insns2[3], POP)) {
						m.instructions.remove(insns2[3]);
						InsnList before = new InsnList();
						before.add(new VarInsnNode(ALOAD, 0));
						before.add(new VarInsnNode(ALOAD, 0));
						before.add(new FieldInsnNode(GETFIELD, idField.owner, idField.name, idField.desc));
						m.instructions.insertBefore(insn, before);
						InsnList after = new InsnList();
						after.add(new MethodInsnNode(INVOKESTATIC, "org/mcphackers/launchwrapper/inject/Inject", "saveLevel", "(ILjava/lang/String;)Ljava/io/File;"));
						after.add(new MethodInsnNode(INVOKEVIRTUAL, nameLevelMenu.name, openFile.name, openFile.desc));
						m.instructions.insert(insns2[2], after);
					}
					insn = nextInsn(insn);
				}
			}
		}
		LabelNode label = new LabelNode();
		InsnList insns = new InsnList();
		insns.add(new VarInsnNode(ALOAD, 1));
		insns.add(new JumpInsnNode(IFNONNULL, label));
		insns.add(new InsnNode(RETURN));
		insns.add(label);
		openFile.instructions.insert(insns);
		nameLevelMenu.methods.add(openFile);
		source.overrideClass(loadLevelMenu);
		source.overrideClass(nameLevelMenu);
		debugInfo("Indev save patch");
	}
	
	private void removeCanvas(MethodNode method) {
		AbstractInsnNode insn1 = method.instructions.getFirst();
		while(insn1 != null) {
			AbstractInsnNode[] insns = fill(insn1, 6);
			if(compareInsn(insns[0], ALOAD)
			&& compareInsn(insns[1], ALOAD)
			&& compareInsn(insns[2], GETFIELD, minecraft.name, width.name, width.desc)
			&& compareInsn(insns[3], ALOAD)
			&& compareInsn(insns[4], GETFIELD, minecraft.name, height.name, height.desc)
			&& compareInsn(insns[5], INVOKESPECIAL, minecraft.name, null, "(II)V")) {
				supportsResizing = true;
			}
			if(compareInsn(insns[0], ALOAD)
			&& compareInsn(insns[1], GETFIELD, minecraft.name, null, "Ljava/awt/Canvas;")
    		&& compareInsn(insns[2], INVOKEVIRTUAL, "java/awt/Canvas", "getWidth", "()I")) {
				MethodInsnNode invoke = new MethodInsnNode(INVOKESTATIC, "org/lwjgl/opengl/Display", "getWidth", "()I");
				method.instructions.insert(insns[2], invoke);
				method.instructions.remove(insns[0]);
				method.instructions.remove(insns[1]);
				method.instructions.remove(insns[2]);
				insn1 = invoke;
				debugInfo("Replaced canvas getWidth");
			}
			if(compareInsn(insns[0], ALOAD)
			&& compareInsn(insns[1], GETFIELD, minecraft.name, null, "Ljava/awt/Canvas;")
    		&& compareInsn(insns[2], INVOKEVIRTUAL, "java/awt/Canvas", "getHeight", "()I")) {
				MethodInsnNode invoke = new MethodInsnNode(INVOKESTATIC, "org/lwjgl/opengl/Display", "getHeight", "()I");
				method.instructions.insert(insns[2], invoke);
				method.instructions.remove(insns[0]);
				method.instructions.remove(insns[1]);
				method.instructions.remove(insns[2]);
				insn1 = invoke;
				debugInfo("Replaced canvas getHeight");
			}
			insn1 = nextInsn(insn1);
    	}
		insn1 = method.instructions.getFirst();
		while(insn1 != null) {
			AbstractInsnNode[] insns = fill(insn1, 6);
			if(compareInsn(insns[0], ALOAD)
			&& compareInsn(insns[1], GETFIELD, minecraft.name, null, "Ljava/awt/Canvas;")
			&& compareInsn(insns[2], IFNULL)
			&& compareInsn(insns[3], ALOAD)
			&& compareInsn(insns[4], GETFIELD, minecraft.name, null, "Z")
			&& compareInsn(insns[5], IFNE)) {
				if(((JumpInsnNode)insns[2]).label != ((JumpInsnNode)insns[5]).label) {
					continue;
				}
				method.instructions.remove(insns[0]);
				method.instructions.remove(insns[1]);
				method.instructions.remove(insns[2]);
				debugInfo("Removed canvas null check");
				break;
			}
			insn1 = nextInsn(insn1);
		}
	}

	private void displayPatch(MethodNode init, boolean supportsResizing) {
		boolean classic = isClassic();
		String canvasName = null;
    	
		int thisIndex = 0;
		LabelNode aLabel = new LabelNode();
		LabelNode iLabel = null;
		LabelNode oLabel = null;
		
		AbstractInsnNode afterLabel = null;
		
		JumpInsnNode ifNoCanvas = null;
		JumpInsnNode ifFullscreen = null;
		
		InsnList insnList = init.instructions;
		AbstractInsnNode insn = insnList.getFirst();
		while(insn != null) {
			AbstractInsnNode[] insns = fill(insn, 6);
			if(iLabel == null
			&& compareInsn(insns[0], ALOAD)
			&& compareInsn(insns[1], GETFIELD, minecraft.name, null, "Ljava/awt/Canvas;")
    		&& compareInsn(insns[2], IFNULL)) {
				thisIndex = ((VarInsnNode)insns[0]).var;
				canvasName = ((FieldInsnNode)insns[1]).name;
				ifNoCanvas = (JumpInsnNode)insns[2];
				iLabel = ifNoCanvas.label;
				afterLabel = insns[0];
				insn = insns[2];
    		}
			if(iLabel == null
			&& compareInsn(insns[0], ALOAD)
			&& compareInsn(insns[1], DUP)
			&& compareInsn(insns[2], ASTORE)
			&& compareInsn(insns[3], GETFIELD, minecraft.name, null, "Ljava/awt/Canvas;")
    		&& compareInsn(insns[4], IFNULL)) {
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
			if(compareInsn(insns[0], NEW, "org/lwjgl/opengl/DisplayMode")
			&& compareInsn(insns[1], DUP)
    		&& compareInsn(insns[2], SIPUSH)
    		&& compareInsn(insns[3], SIPUSH)
    		&& compareInsn(insns[4], INVOKESPECIAL, "org/lwjgl/opengl/DisplayMode", "<init>", "(II)V")
    		&& compareInsn(insns[5], INVOKESTATIC, "org/lwjgl/opengl/Display", "setDisplayMode", "(Lorg/lwjgl/opengl/DisplayMode;)V")) {
				debugInfo("Pre-classic resolution patch");
    			InsnList insert = getIcon(classic);
    			if(launch.forceVsync.get()) {
    				insert.add(new InsnNode(ICONST_1));
    				insert.add(new MethodInsnNode(INVOKESTATIC, "org/lwjgl/opengl/Display", "setVSyncEnabled", "(Z)V"));
    				debugInfo("Forced VSync");
    			}
				if(!launch.fullscreen.get()) {
    				insnList.insert(insns[2], intInsn(launch.width.get()));
    				insnList.insert(insns[3], intInsn(launch.height.get()));
        			insnList.insert(insns[5], insert);
    				insnList.remove(insns[2]);
    				insnList.remove(insns[3]);
				} else {
        			insert.add(new InsnNode(ICONST_1));
        			insert.add(new MethodInsnNode(INVOKESTATIC, "org/lwjgl/opengl/Display", "setFullscreen", "(Z)V"));
        			insnList.insert(insns[5], insert);
    				removeRange(insnList, insns[0], insns[5]);
				}
    		}
			// rd-152252
			else if(compareInsn(insns[0], ICONST_1)
    		&& compareInsn(insns[1], INVOKESTATIC, "org/lwjgl/opengl/Display", "setFullscreen", "(Z)V")
    		&& insns[2] != null && insns[2].getType() == AbstractInsnNode.LABEL
    		&& compareInsn(insns[4], INVOKESTATIC, "org/lwjgl/opengl/Display", "create", "()V")) {
				debugInfo("Pre-classic resolution patch");
    			InsnList insert = getIcon(classic);
    			if(launch.forceVsync.get()) {
    				insert.add(new InsnNode(ICONST_1));
    				insert.add(new MethodInsnNode(INVOKESTATIC, "org/lwjgl/opengl/Display", "setVSyncEnabled", "(Z)V"));
    				debugInfo("Forced VSync");
    			}
				if(!launch.fullscreen.get()) {
					insert.add(new TypeInsnNode(NEW, "org/lwjgl/opengl/DisplayMode"));
					insert.add(new InsnNode(DUP));
					insert.add(intInsn(launch.width.get()));
					insert.add(intInsn(launch.height.get()));
					insert.add(new MethodInsnNode(INVOKESPECIAL, "org/lwjgl/opengl/DisplayMode", "<init>", "(II)V"));
					insert.add(new MethodInsnNode(INVOKESTATIC, "org/lwjgl/opengl/Display", "setDisplayMode", "(Lorg/lwjgl/opengl/DisplayMode;)V"));
        			insnList.insert(insns[1], insert);
        			insnList.remove(insns[0]);
        			insnList.remove(insns[1]);
				} else {
        			insnList.insert(insns[1], insert);
				}
    		}
			
    		if(oLabel == null
    		&& compareInsn(insns[0], ICONST_1)
    		&& compareInsn(insns[1], INVOKESTATIC, "org/lwjgl/opengl/Display", "setFullscreen", "(Z)V")) {
	    		AbstractInsnNode insn2 = insns[0];
	    		while(insn2 != null) {
	    			if(insn2.getOpcode() == IFEQ) {
    	    			if(compareInsn(insn2.getPrevious(), GETFIELD)) {
    	    				fullscreenField = (FieldInsnNode)insn2.getPrevious();
    	    			}
	    	    		ifFullscreen = (JumpInsnNode)insn2;
	    				oLabel = ifFullscreen.label;
	    			}
	    			insn2 = previousInsn(insn2);
	    		}
    		}
    		if(insn.getOpcode() == LDC) {
    			LdcInsnNode ldc = (LdcInsnNode)insn;
    			if(ldc.cst instanceof String) {
    				String value = (String)ldc.cst;
    				if(value.startsWith("Minecraft Minecraft")) {
    					debugInfo("Fixed title");
    					ldc.cst = value.substring(10);
    				}
    			}
    		}
    		if(canvasName != null) {
    			boolean found
    			 = compareInsn(insns[0], ALOAD, thisIndex)
    			&& compareInsn(insns[1], GETFIELD, minecraft.name, canvasName, "Ljava/awt/Canvas;")
	    		&& compareInsn(insns[2], INVOKESPECIAL, null, "<init>", "(Ljava/awt/Component;)V");

    			if(found
	    		|| compareInsn(insns[0], ALOAD, thisIndex)
	    		&& compareInsn(insns[1], GETFIELD, minecraft.name, canvasName, "Ljava/awt/Canvas;")
  			  	&& compareInsn(insns[2], ALOAD, thisIndex)
  			  	&& compareInsn(insns[3], GETFIELD, minecraft.name)
	    		&& compareInsn(insns[4], INVOKESPECIAL, null, "<init>")) {
    				mouseHelperName = found ? ((MethodInsnNode)insns[2]).owner : ((MethodInsnNode)insns[4]).owner;
    			}
			}
			insn = nextInsn(insn);
		}
		
		if(afterLabel != null
	    && iLabel != null
	    && oLabel != null
	    && ifNoCanvas != null
	    && ifFullscreen != null) {
			debugInfo("Fullscreen init patch");
			insnList.insertBefore(afterLabel, aLabel);
			InsnList insert = new InsnList();
			insert.add(getIcon(classic));
			if(supportsResizing) {
				insert.add(new InsnNode(ICONST_1));
				insert.add(new MethodInsnNode(INVOKESTATIC, "org/lwjgl/opengl/Display", "setResizable", "(Z)V"));
			}
			if(launch.forceVsync.get()) {
				insert.add(new InsnNode(ICONST_1));
				insert.add(new MethodInsnNode(INVOKESTATIC, "org/lwjgl/opengl/Display", "setVSyncEnabled", "(Z)V"));
				debugInfo("Forced VSync");
			}
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
		}
    	
    	if(fullscreenField != null) {
    		methodLoop:
    		for(MethodNode m : minecraft.methods) {
				AbstractInsnNode insn2 = m.instructions.getFirst();
				while(insn2 != null) {
					if(insn2.getOpcode() == GETFIELD) {
						if(compareInsn(insn2, GETFIELD, minecraft.name, fullscreenField.name, fullscreenField.desc)) {
							break;
						} else {
							continue methodLoop;
						}
					}
					insn2 = nextInsn(insn2);
				}
				while(insn2 != null) {
					AbstractInsnNode[] insns2 = fill(insn2, 4);
	    			if(compareInsn(insns2[0], ALOAD)
	    			&& compareInsn(insns2[1], ALOAD)
					&& compareInsn(insns2[2], GETFIELD, minecraft.name, null, width.desc)
					&& compareInsn(insns2[3], PUTFIELD, minecraft.name, width.name, width.desc)) {
	    	    		defaultWidth = (FieldInsnNode)insns2[2];
	    			}
	    			if(compareInsn(insns2[0], ALOAD)
	    			&& compareInsn(insns2[1], ALOAD)
					&& compareInsn(insns2[2], GETFIELD, minecraft.name, null, height.desc)
					&& compareInsn(insns2[3], PUTFIELD, minecraft.name, height.name, height.desc)) {
	    	    		defaultHeight = (FieldInsnNode)insns2[2];
	    			}
	    			if(defaultWidth != null && defaultHeight != null
	    			&& compareInsn(insns2[0], IFGT)
	    			&& compareInsn(insns2[1], ALOAD)
					&& compareInsn(insns2[2], ICONST_1)
					&& compareInsn(insns2[3], PUTFIELD, minecraft.name, height.name, height.desc)) {
	    				AbstractInsnNode next = nextInsn(insns2[3]);
	    				debugInfo("Fullscreen toggle patch");
						AbstractInsnNode[] insns3 = fill(next, 8);
						if(compareInsn(insns3[0], NEW, "org/lwjgl/opengl/DisplayMode")
						&& compareInsn(insns3[1], DUP)
						&& compareInsn(insns3[2], ALOAD)
						&& compareInsn(insns3[3], GETFIELD, minecraft.name, null, defaultWidth.desc)
						&& compareInsn(insns3[4], ALOAD)
						&& compareInsn(insns3[5], GETFIELD, minecraft.name, null, defaultHeight.desc)
						&& compareInsn(insns3[6], INVOKESPECIAL, "org/lwjgl/opengl/DisplayMode", "<init>", "(II)V")
						&& compareInsn(insns3[7], INVOKESTATIC, "org/lwjgl/opengl/Display", "setDisplayMode", "(Lorg/lwjgl/opengl/DisplayMode;)V")) {
							m.instructions.set(insns3[3], new FieldInsnNode(GETFIELD, minecraft.name, defaultWidth.name, defaultWidth.desc));
							m.instructions.set(insns3[5], new FieldInsnNode(GETFIELD, minecraft.name, defaultHeight.name, defaultHeight.desc));
    	    			} else {
		    				JumpInsnNode jump = (JumpInsnNode)insns2[0];
		    				LabelNode newLabel = new LabelNode();
		    				jump.label = newLabel;
		    				InsnList insert = new InsnList();
		    				insert.add(newLabel);
		    				insert.add(new TypeInsnNode(NEW, "org/lwjgl/opengl/DisplayMode"));
		    				insert.add(new InsnNode(DUP));
		    				insert.add(new VarInsnNode(ALOAD, 0));
		    				insert.add(new FieldInsnNode(GETFIELD, minecraft.name, defaultWidth.name, defaultWidth.desc));
		    				insert.add(new VarInsnNode(ALOAD, 0));
		    				insert.add(new FieldInsnNode(GETFIELD, minecraft.name, defaultHeight.name, defaultHeight.desc));
		    				insert.add(new MethodInsnNode(INVOKESPECIAL, "org/lwjgl/opengl/DisplayMode", "<init>", "(II)V"));
		    				insert.add(new MethodInsnNode(INVOKESTATIC, "org/lwjgl/opengl/Display", "setDisplayMode", "(Lorg/lwjgl/opengl/DisplayMode;)V"));
		    	    		m.instructions.insert(insns2[3], insert);
    	    			}
	    			}
					insn2 = nextInsn(insn2);
				}
    		}
    	}
	}

	private void patchMinecraftInit() {
		for(MethodNode m : minecraft.methods) {
			if(m.name.equals("<init>")) {
	    		InsnList insert = new InsnList();
	    		AbstractInsnNode insn = m.instructions.getFirst();
	    		boolean widthReplaced = false;
	    		boolean heightReplaced = false;
	    		boolean fullscreenReplaced = false;
	    		while(insn != null) {
	    			AbstractInsnNode[] insns = fill(insn, 3);
	    			if(fullscreenField != null
	    			&& compareInsn(insns[0], ALOAD)
	    			&& compareInsn(insns[2], PUTFIELD, minecraft.name, fullscreenField.name, fullscreenField.desc)) {
	    				m.instructions.set(insns[1], booleanInsn(launch.fullscreen.get()));
	    				debugInfo("Replaced fullscreen");
	    			}
		    		if(width != null && height != null) {
		    			if(compareInsn(insns[0], ALOAD)
		    			&& compareInsn(insns[2], PUTFIELD, minecraft.name, width.name, width.desc)) {
		    				m.instructions.set(insns[1], intInsn(launch.width.get()));
		    				widthReplaced = true;
	        				debugInfo("Replaced width");
		    			} else
		    			if(compareInsn(insns[0], ALOAD)
		    			&& compareInsn(insns[2], PUTFIELD, minecraft.name, height.name, height.desc)) {
		    				m.instructions.set(insns[1], intInsn(launch.height.get()));
		    				heightReplaced = true;
	        				debugInfo("Replaced height");
		    			}
		    		}
	    			insn = nextInsn(insn);
	    		}
	    		int thisIndex = 0;
	    		if(width != null && height != null) {
					if(!widthReplaced) {
						insert.add(new VarInsnNode(ALOAD, thisIndex));
						insert.add(intInsn(launch.width.get()));
						insert.add(new FieldInsnNode(PUTFIELD, minecraft.name, width.name, width.desc));
	    				debugInfo("Set initial width");
					}
					if(!heightReplaced) {
						insert.add(new VarInsnNode(ALOAD, thisIndex));
						insert.add(intInsn(launch.height.get()));
						insert.add(new FieldInsnNode(PUTFIELD, minecraft.name, height.name, height.desc));
	    				debugInfo("Set initial height");
					}
	    		}
	    		if(!fullscreenReplaced && fullscreenField != null) {
					insert.add(new VarInsnNode(ALOAD, thisIndex));
					insert.add(booleanInsn(launch.fullscreen.get()));
					insert.add(new FieldInsnNode(PUTFIELD, minecraft.name, fullscreenField.name, fullscreenField.desc));
					debugInfo("Set fullscreen");
	    		}
	    		if(defaultWidth != null && defaultHeight != null) {
					debugInfo("Set default width and height");
					insert.add(new VarInsnNode(ALOAD, thisIndex));
					insert.add(intInsn(launch.width.get()));
					insert.add(new FieldInsnNode(PUTFIELD, minecraft.name, defaultWidth.name, defaultWidth.desc));
					insert.add(new VarInsnNode(ALOAD, thisIndex));
					insert.add(intInsn(launch.height.get()));
					insert.add(new FieldInsnNode(PUTFIELD, minecraft.name, defaultHeight.name, defaultHeight.desc));
	    		}
				m.instructions.insert(getSuper(m.instructions.getFirst()), insert);
			}
		}
	}

	private void enableLegacyMergeSort() {
		try {
			Class<?> mergeSort = Class.forName("java.util.Arrays$LegacyMergeSort");
			Field userRequested = mergeSort.getDeclaredField("userRequested");
			Util.setStaticBooleanUnsafe(userRequested, true);
		} catch (ClassNotFoundException e) {
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void fixMouseHelper(String mouseHelperName) {
		if(!launch.lwjglFrame.get() || mouseHelperName == null) {
			return;
		}
		long i = System.currentTimeMillis();
		ClassNode mouseHelper = source.getClass(mouseHelperName);
		MethodNode setDelta = null;
		MethodNode setGrabbed = null;
		String dx = null;
		String dy = null;
		method:
		for(MethodNode m : mouseHelper.methods) {
			if(!m.desc.equals("()V") || m.name.equals("<init>") || m.name.equals("<clinit>")) {
				continue;
			}
			AbstractInsnNode insn = m.instructions.getFirst();
			while(insn != null) {
    			AbstractInsnNode[] insns = fill(insn, 4);
    			if(compareInsn(insns[0], INVOKESTATIC, "java/awt/MouseInfo", "getPointerInfo", "()Ljava/awt/PointerInfo;")
    	    	&& compareInsn(insns[1], INVOKEVIRTUAL, "java/awt/PointerInfo", "getLocation", "()Ljava/awt/Point;")) {
    				setDelta = m;
    				while(insn != null) {
    					insns = fill(insn, 7);
    	    			if(compareInsn(insns[0], ALOAD)
    	    	    	&& compareInsn(insns[1], ALOAD)
    	    	    	&& compareInsn(insns[2], GETFIELD, "java/awt/Point", null, "I")
    	    	    	&& compareInsn(insns[3], ALOAD)
    	    	    	&& compareInsn(insns[4], GETFIELD, mouseHelper.name, null, "I")
    	    	    	&& compareInsn(insns[5], ISUB)
    	    	    	&& compareInsn(insns[6], PUTFIELD, mouseHelper.name, null, "I")) {
    	    				FieldInsnNode point = (FieldInsnNode)insns[2];
    	    				FieldInsnNode putfield = (FieldInsnNode)insns[6];
    	    				if(point.name.equals("x")) {
    	    					dx = putfield.name;
    	    				}
    	    				else if(point.name.equals("y")) {
    	    					dy = putfield.name;
    	    				}
    	    			}
    					insn = nextInsn(insn);
    				}
    				continue method;
    			}
    			if(compareInsn(insns[0], GETFIELD, mouseHelper.name, null, "Lorg/lwjgl/input/Cursor;")
    	    	&& compareInsn(insns[1], INVOKESTATIC, "org/lwjgl/input/Mouse", "setNativeCursor", "(Lorg/lwjgl/input/Cursor;)Lorg/lwjgl/input/Cursor;")) {
    				setGrabbed = m;
    				continue method;
    			}
				insn = nextInsn(insn);
			}
		}
		if(setDelta != null && setGrabbed != null && dx != null && dy != null) {
			setGrabbed.localVariables = null;
			setGrabbed.tryCatchBlocks.clear();
			InsnList insns = new InsnList();
			insns.add(new InsnNode(ICONST_1));
			insns.add(new MethodInsnNode(INVOKESTATIC, "org/lwjgl/input/Mouse", "setGrabbed", "(Z)V"));
			insns.add(new VarInsnNode(ALOAD, 0));
			insns.add(new InsnNode(ICONST_0));
			insns.add(new FieldInsnNode(PUTFIELD, mouseHelper.name, dx, "I"));
			insns.add(new VarInsnNode(ALOAD, 0));
			insns.add(new InsnNode(ICONST_0));
			insns.add(new FieldInsnNode(PUTFIELD, mouseHelper.name, dy, "I"));
			insns.add(new InsnNode(RETURN));
			setGrabbed.instructions = insns;

			setDelta.localVariables = null;
			setDelta.tryCatchBlocks.clear();
			insns = new InsnList();
			insns.add(new VarInsnNode(ALOAD, 0));
			insns.add(new MethodInsnNode(INVOKESTATIC, "org/lwjgl/input/Mouse", "getDX", "()I"));
			insns.add(new FieldInsnNode(PUTFIELD, mouseHelper.name, dx, "I"));
			insns.add(new VarInsnNode(ALOAD, 0));
			insns.add(new MethodInsnNode(INVOKESTATIC, "org/lwjgl/input/Mouse", "getDY", "()I"));
			insns.add(new FieldInsnNode(PUTFIELD, mouseHelper.name, dy, "I"));
			insns.add(new InsnNode(RETURN));
			setDelta.instructions = insns;
			
			for(MethodNode m : minecraft.methods) {
				if(!m.desc.equals("()V") || m.tryCatchBlocks.isEmpty()) {
					continue;
				}
				for(TryCatchBlockNode tryCatch : m.tryCatchBlocks) {
					if(tryCatch.type != null && tryCatch.type.equals("org/lwjgl/LWJGLException")) {
						AbstractInsnNode[] insns2 = fill(nextInsn(tryCatch.start), 3);
    	    			if(compareInsn(insns2[0], ACONST_NULL)
    	    	    	&& compareInsn(insns2[1], INVOKESTATIC, "org/lwjgl/input/Mouse", "setNativeCursor", "(Lorg/lwjgl/input/Cursor;)Lorg/lwjgl/input/Cursor;")
    	    	    	&& compareInsn(insns2[2], POP)) {
    	    				insns = new InsnList();
    	    				insns.add(new MethodInsnNode(INVOKESTATIC, "org/lwjgl/opengl/Display", "getWidth", "()I"));
    	    				insns.add(new InsnNode(ICONST_2));
    	    				insns.add(new InsnNode(IDIV));
    	    				insns.add(new MethodInsnNode(INVOKESTATIC, "org/lwjgl/opengl/Display", "getHeight", "()I"));
    	    				insns.add(new InsnNode(ICONST_2));
    	    				insns.add(new InsnNode(IDIV));
    	    				insns.add(new MethodInsnNode(INVOKESTATIC, "org/lwjgl/input/Mouse", "setCursorPosition", "(II)V"));
    	    				insns.add(new InsnNode(ICONST_0));
    	    				insns.add(new MethodInsnNode(INVOKESTATIC, "org/lwjgl/input/Mouse", "setGrabbed", "(Z)V"));
    	    				m.instructions.insertBefore(insns2[0], insns);
    	    				removeRange(m.instructions, insns2[0], insns2[2]);
    	    				m.tryCatchBlocks.remove(tryCatch);
    	    				break;
    	    			}
					}
				}
			}
			for(FieldNode field : minecraft.fields) {
				if(field.desc.startsWith("L") && field.desc.endsWith(";")) {
					boolean success = false;
					ClassNode node = source.getClass(field.desc.substring(1, field.desc.length() - 1));
					if(node == null) {
						continue;
					}
					method:
					for(MethodNode m : node.methods) {
						AbstractInsnNode insn = m.instructions.getFirst();
						while(insn != null) {
							if(insn.getOpcode() == INVOKESTATIC) {
				    			AbstractInsnNode[] insns2 = fill(insn, 3);
				    			if(
				    			(  compareInsn(insns2[0], INVOKESTATIC, "org/lwjgl/input/Mouse", "getDX", "()I")
				    			|| compareInsn(insns2[0], INVOKESTATIC, "org/lwjgl/input/Mouse", "getDY", "()I") )
		    	    			&& compareInsn(insns2[1], POP)) {
				    				m.instructions.remove(insns2[0]);
				    				m.instructions.remove(insns2[1]);
				    				insn = insns2[2];
				    				success = true;
		    	    			}
							}
							else {
				    			AbstractInsnNode[] insns2 = fill(insn, 3);
				    			if(compareInsn(insns2[0], GETFIELD, minecraft.name, null, "L" + mouseHelper.name + ";")
				    			&& compareInsn(insns2[1], GETFIELD, mouseHelper.name, dy, "I")
		    	    			&& compareInsn(insns2[2], ISUB)) {
				    				m.instructions.set(insns2[2], new InsnNode(IADD));
				    				success = true;
				    				break method;
		    	    			}
							}
							insn = nextInsn(insn);
						}
					}
					if(success) {
						debugInfo("Extra MouseHelper fix");
						source.overrideClass(node);
						break;
					}
				}
			}
		}
		else {
			for(MethodNode m : mouseHelper.methods) {
				AbstractInsnNode insn = m.instructions.getFirst();
				while(insn != null) {
	    			AbstractInsnNode[] insns = fill(insn, 4);
	    			
	    			if(compareInsn(insns[0], ALOAD)
	    			&& compareInsn(insns[1], GETFIELD, mouseHelper.name, null, "Ljava/awt/Component;")
	    			&& compareInsn(insns[2], INVOKEVIRTUAL, "java/awt/Component", "getParent", "()Ljava/awt/Container;")
	    			&& compareInsn(insns[3], IFNULL)) {
	    				LabelNode gotoLabel = ((JumpInsnNode)insns[3]).label;
	    				m.instructions.insertBefore(insns[0], new JumpInsnNode(GOTO, gotoLabel));
	    			}
	    			
	    			if(compareInsn(insns[0], ALOAD)
	    			&& compareInsn(insns[1], GETFIELD, mouseHelper.name, null, "Ljava/awt/Component;")
	        		&& compareInsn(insns[2], INVOKEVIRTUAL, "java/awt/Component", "getWidth", "()I")) {
	    				MethodInsnNode invoke = new MethodInsnNode(INVOKESTATIC, "org/lwjgl/opengl/Display", "getWidth", "()I");
	    				m.instructions.insert(insns[2], invoke);
	    				m.instructions.remove(insns[0]);
	    				m.instructions.remove(insns[1]);
	    				m.instructions.remove(insns[2]);
	    				insn = invoke;
	    			}
	    			if(compareInsn(insns[0], ALOAD)
	    			&& compareInsn(insns[1], GETFIELD, mouseHelper.name, null, "Ljava/awt/Component;")
	        		&& compareInsn(insns[2], INVOKEVIRTUAL, "java/awt/Component", "getHeight", "()I")) {
	    				MethodInsnNode invoke = new MethodInsnNode(INVOKESTATIC, "org/lwjgl/opengl/Display", "getHeight", "()I");
	    				m.instructions.insert(insns[2], invoke);
	    				m.instructions.remove(insns[0]);
	    				m.instructions.remove(insns[1]);
	    				m.instructions.remove(insns[2]);
	    				insn = invoke;
	    			}
	    			if(compareInsn(insns[0], ALOAD)
	    			&& compareInsn(insns[1], GETFIELD, mouseHelper.name, null, "Ljava/awt/Component;")
	        		&& compareInsn(insns[2], INVOKEVIRTUAL, "java/awt/Component", "getLocationOnScreen", "()Ljava/awt/Point;")) {
	    				InsnList insert = new InsnList();
	    				insert.add(new TypeInsnNode(NEW, "java/awt/Point"));
	    				insert.add(new InsnNode(DUP));
	    				insert.add(new MethodInsnNode(INVOKESTATIC, "org/lwjgl/opengl/Display", "getX", "()I"));
	    				insert.add(new MethodInsnNode(INVOKESTATIC, "org/lwjgl/opengl/Display", "getY", "()I"));
	    				insert.add(new MethodInsnNode(INVOKESPECIAL, "java/awt/Point", "<init>", "(II)V"));
	    				insn = insert.getLast();
	    				m.instructions.insert(insns[2], insert);
	    				m.instructions.remove(insns[0]);
	    				m.instructions.remove(insns[1]);
	    				m.instructions.remove(insns[2]);
	    			}
					insn = nextInsn(insn);
				}
			}
		}
		debugInfo("MouseHelper fixed in " + (System.currentTimeMillis() - i) + " ms");
		source.overrideClass(mouseHelper);
	}

	private void fixA111GrayScreen() {
		for(MethodNode m : minecraft.methods) {
			if(m.desc.equals("()V")) {
	    		AbstractInsnNode insn = m.instructions.getFirst();
	    		while(insn != null) {
	    			if(compareInsn(insn, INVOKESTATIC, "org/lwjgl/opengl/Display", "setDisplayConfiguration", "(FFF)V")) {
		    			AbstractInsnNode[] insns = fillBackwards(insn, 4);
		    			if(compareInsn(insns[0], FCONST_1)
		    			&& compareInsn(insns[1], FCONST_0)
		    			&& compareInsn(insns[2], FCONST_0)) {
			    			m.instructions.insertBefore(insns[0], new InsnNode(NOP));
			    			removeRange(m.instructions, insns[0], insn);
		    				debugInfo("Fixed gray screen");
		    				return;
	    				}
	    			}
	    			if(insn.getOpcode() == INVOKESTATIC) {
	    				return;
	    			}
	    			insn = nextInsn(insn);
	    		}
			}
		}
	}

	private void fixShutdown(MethodNode run) {
		MethodNode destroy = null;
		if(run != null) {
			AbstractInsnNode insn1 = run.instructions.getLast();
			while(insn1 != null && insn1.getOpcode() != ATHROW) {
				insn1 = previousInsn(insn1);
			}
			AbstractInsnNode[] insns1 = fillBackwards(insn1, 4);
			if(compareInsn(insns1[3], ATHROW)
			&& compareInsn(insns1[1], INVOKEVIRTUAL, minecraft.name, null, "()V")
			&& compareInsn(insns1[0], ALOAD)
			&& compareInsn(insns1[2], ALOAD)) {
				MethodInsnNode invoke = (MethodInsnNode)insns1[1];
				destroy = InjectUtils.getMethod(minecraft, invoke.name, invoke.desc);
			}
		}
		if(destroy == null) {
			for(MethodNode m : minecraft.methods) {
				if(containsInvoke(m.instructions, new MethodInsnNode(INVOKESTATIC, "org/lwjgl/input/Mouse", "destroy", "()V"))
				&& containsInvoke(m.instructions, new MethodInsnNode(INVOKESTATIC, "org/lwjgl/input/Keyboard", "destroy", "()V"))
				&& containsInvoke(m.instructions, new MethodInsnNode(INVOKESTATIC, "org/lwjgl/opengl/Display", "destroy", "()V"))) {
					destroy = m;
					debugInfo(destroy.name + destroy.desc + " is the destroy() method");
					break;
				}
			}
		}
		AbstractInsnNode insn1 = run.instructions.getFirst();
		if(destroy != null) {
			while(insn1 != null) {
				if(insn1.getOpcode() == RETURN
				&& !compareInsn(insn1.getPrevious(), INVOKEVIRTUAL, minecraft.name, destroy.name, destroy.desc)) {
					InsnList insert = new InsnList();
					insert.add(new VarInsnNode(ALOAD, 0));
					insert.add(new MethodInsnNode(INVOKEVIRTUAL, minecraft.name, destroy.name, destroy.desc));
					run.instructions.insertBefore(insn1, insert);
				}
				insn1 = nextInsn(insn1);
			}
			insn1 = destroy.instructions.getFirst();
			while(insn1 != null) {
				if(compareInsn(insn1, INVOKESTATIC, "org/lwjgl/opengl/Display", "destroy", "()V")) {
					AbstractInsnNode insn3 = nextInsn(insn1);
					if(insn3 != null) {
						AbstractInsnNode[] insns2 = fill(insn3, 2);
						if(compareInsn(insns2[0], ICONST_0)
						&& compareInsn(insns2[1], INVOKESTATIC, "java/lang/System", "exit", "(I)V")) {
						} else {
							InsnList insert = new InsnList();
							insert.add(new InsnNode(ICONST_0));
							insert.add(new MethodInsnNode(INVOKESTATIC, "java/lang/System", "exit", "(I)V"));
							destroy.instructions.insert(insn1, insert);
		    				debugInfo("Shutdown patch");
						}
					}
				}
				insn1 = nextInsn(insn1);
			}

			boolean setWorldIsWrapped = false;
			for(TryCatchBlockNode tryCatch : destroy.tryCatchBlocks) {
				AbstractInsnNode insn = nextInsn(tryCatch.start);
				AbstractInsnNode[] insns2 = fill(insn, 3);
				if(compareInsn(insns2[0], ALOAD)
				&& compareInsn(insns2[1], ACONST_NULL)
				&& compareInsn(insns2[2], INVOKEVIRTUAL, minecraft.name, null, null)) {
					MethodInsnNode invoke = (MethodInsnNode)insns2[2];
					if(Type.getReturnType(invoke.desc).getSort() == Type.VOID) {
						setWorldIsWrapped = true;
						break;
					}
				}
			}
			if(!setWorldIsWrapped) {
				insn1 = destroy.instructions.getFirst();
				while(insn1 != null) {
					AbstractInsnNode[] insns2 = fill(insn1, 3);
					if(compareInsn(insns2[0], ALOAD)
					&& compareInsn(insns2[1], ACONST_NULL)
					&& compareInsn(insns2[2], INVOKEVIRTUAL, minecraft.name, null, null)) {
						MethodInsnNode invoke = (MethodInsnNode)insns2[2];
						if(Type.getReturnType(invoke.desc).getSort() == Type.VOID) {
							tryCatchAdd(destroy, insns2[0], insns2[2], "java/lang/Throwable");
							debugInfo("Fixed sound manager shutdown (b1.0 - b1.3)");
							break;
						}
					}
					insn1 = nextInsn(insn1);
				}
			}
		}
	}

	private void fixIndevLaunch() {
		MethodNode setLevel = null;
		for(MethodNode m : minecraft.methods) {
    		AbstractInsnNode insn = m.instructions.getLast();
    		if(insn == null) continue;
    		AbstractInsnNode insn2 = insn.getPrevious();
			if(compareInsn(insn2, INVOKESTATIC, "java/lang/System", "gc", "()V")
			&& compareInsn(insn, RETURN)) {
				setLevel = m;
				break;
			}
		}
		if(setLevel != null) {
			if(setLevel.tryCatchBlocks.size() > 0) {
				TryCatchBlockNode handler = setLevel.tryCatchBlocks.get(0);
				LabelNode lbl = handler.end;
				LabelNode lbl2 = null;
	    		AbstractInsnNode insn = lbl.getPrevious();
	    		while(insn != null) {
	    			if(insn.getType() == AbstractInsnNode.LABEL) {
	    				lbl2 = (LabelNode)insn;
	    				break;
	    			}
	    			insn = insn.getPrevious();
	    		}
	    		if(lbl2 != null) {
	    			debugInfo("Indev launch tweak");
	    			removeRange(setLevel.instructions, handler.start.getNext(), lbl2);
	    			setLevel.tryCatchBlocks.remove(handler);
	    		}
			}
		}
	}

	private MethodNode getInit(MethodNode run) {
		for(AbstractInsnNode insn : run.instructions) {
			if(insn.getType() == AbstractInsnNode.METHOD_INSN) {
				if(insn.getOpcode() != INVOKEVIRTUAL) {
					break;
				}
			}
			if(compareInsn(insn, INVOKEVIRTUAL, minecraft.name)) {
				MethodInsnNode invoke = (MethodInsnNode)insn;
				return InjectUtils.getMethod(minecraft, invoke.name, invoke.desc);
			}
		}
		return run;
	}

	private MethodNode getTickMethod(MethodNode run) {
		if(running == null) {
			return run;
		}
		AbstractInsnNode insn = run.instructions.getFirst();
		while(insn != null) {
			AbstractInsnNode[] insns = fill(insn, 3);
			if(compareInsn(insns[0], ALOAD)
			&& compareInsn(insns[1], GETFIELD, minecraft.name, running.name, running.desc)
    		&& compareInsn(insns[2], IFEQ)) {
	    		while(insn != null) {
	    			if(compareInsn(insn.getPrevious(), ALOAD, 0)
	    			&& compareInsn(insn, INVOKESPECIAL, minecraft.name, null, "()V")) {
	    				MethodInsnNode invoke = (MethodInsnNode)insn;
	    				MethodNode testedMethod = InjectUtils.getMethod(minecraft, invoke.name, invoke.desc);
	    				if(testedMethod != null) {
	    					AbstractInsnNode insn2 = testedMethod.instructions.getFirst();
	    					while(insn2 != null) {
	    						insns = fill(insn2, 3);
	    						if(compareInsn(insns[0], ALOAD)
	    						&& compareInsn(insns[1], ICONST_0)
	    						&& compareInsn(insns[2], PUTFIELD, minecraft.name, running.name, running.desc)) {
	    							return testedMethod;
	    						}
	    						insn2 = nextInsn(insn2);
	    					}
	    				}
	    			}
	    			insn = nextInsn(insn);
	    		}
				break;
			}
			insn = nextInsn(insn);
		}
		return run;
	}

	private InsnList getIcon(boolean useDefault) {
		debugInfo("Replaced icon");
		InsnList insert = new InsnList();
		insert.add(booleanInsn(useDefault));
		insert.add(new MethodInsnNode(INVOKESTATIC, "org/mcphackers/launchwrapper/inject/Inject", "loadIcons", "(Z)[Ljava/nio/ByteBuffer;"));
		insert.add(new MethodInsnNode(INVOKESTATIC, "org/lwjgl/opengl/Display", "setIcon", "([Ljava/nio/ByteBuffer;)I"));
		insert.add(new InsnNode(POP));
		return insert;
	}

	private MethodNode getMain() {
		MethodNode node = new MethodNode(ACC_PUBLIC | ACC_STATIC, "main", "([Ljava/lang/String;)V", null, null);
		InsnList insns = node.instructions;
		
		final int appletIndex = 1;
		final int frameIndex = 2;
		final int canvasIndex = 3;
		final int mcIndex = 4;
		final int threadIndex = 5;
		
		final String listenerClass = "org/mcphackers/launchwrapper/inject/WindowListener";

		String mcField = null;
	    String mcDesc = "L" + minecraft.name + ";";
		if(minecraftApplet != null) {
		    for(FieldNode field : minecraftApplet.fields) {
		    	if(mcDesc.equals(field.desc)) {
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
			patchAppletInit();
		}
		if(!launch.lwjglFrame.get()) {
			insns.add(new TypeInsnNode(NEW, "java/awt/Frame"));
			insns.add(new InsnNode(DUP));
			insns.add(new LdcInsnNode("Minecraft"));
			insns.add(new MethodInsnNode(INVOKESPECIAL, "java/awt/Frame", "<init>", "(Ljava/lang/String;)V"));
			insns.add(new VarInsnNode(ASTORE, frameIndex));
			insns.add(new VarInsnNode(ALOAD, frameIndex));
			insns.add(new FieldInsnNode(INVOKESTATIC, "org/mcphackers/launchwrapper/inject/Inject", "getIcon", "()Ljava/awt/image/BufferedImage;"));
			insns.add(new MethodInsnNode(INVOKEVIRTUAL, "java/awt/Frame", "setIconImage", "(Ljava/awt/Image;)V"));
			insns.add(new TypeInsnNode(NEW, "java/awt/Canvas"));
			insns.add(new InsnNode(DUP));
			insns.add(new MethodInsnNode(INVOKESPECIAL, "java/awt/Canvas", "<init>", "()V"));
			insns.add(new VarInsnNode(ASTORE, canvasIndex));
			insns.add(new VarInsnNode(ALOAD, frameIndex));
			insns.add(new TypeInsnNode(NEW, "java/awt/BorderLayout"));
			insns.add(new InsnNode(DUP));
			insns.add(new MethodInsnNode(INVOKESPECIAL, "java/awt/BorderLayout", "<init>", "()V"));
			insns.add(new MethodInsnNode(INVOKEVIRTUAL, "java/awt/Frame", "setLayout", "(Ljava/awt/LayoutManager;)V"));
			insns.add(new VarInsnNode(ALOAD, frameIndex));
			insns.add(new VarInsnNode(ALOAD, canvasIndex));
			insns.add(new LdcInsnNode("Center"));
			insns.add(new MethodInsnNode(INVOKEVIRTUAL, "java/awt/Frame", "add", "(Ljava/awt/Component;Ljava/lang/Object;)V"));
			insns.add(new VarInsnNode(ALOAD, canvasIndex));
			insns.add(new TypeInsnNode(NEW, "java/awt/Dimension"));
			insns.add(new InsnNode(DUP));
			insns.add(intInsn(launch.width.get()));
			insns.add(intInsn(launch.height.get()));
			insns.add(new MethodInsnNode(INVOKESPECIAL, "java/awt/Dimension", "<init>", "(II)V"));
			insns.add(new MethodInsnNode(INVOKEVIRTUAL, "java/awt/Canvas", "setPreferredSize", "(Ljava/awt/Dimension;)V"));
			insns.add(new VarInsnNode(ALOAD, frameIndex));
			insns.add(new MethodInsnNode(INVOKEVIRTUAL, "java/awt/Frame", "pack", "()V"));
			insns.add(new VarInsnNode(ALOAD, frameIndex));
			insns.add(new InsnNode(ACONST_NULL));
			insns.add(new MethodInsnNode(INVOKEVIRTUAL, "java/awt/Frame", "setLocationRelativeTo", "(Ljava/awt/Component;)V"));
		}
		if(minecraftApplet != null) {
			insns.add(new VarInsnNode(ALOAD, appletIndex));
			insns.add(new FieldInsnNode(GETFIELD, minecraftApplet.name, mcField, mcDesc));
		} else {
			insns.add(getNewMinecraftImpl(minecraft, null));
		}
		insns.add(new VarInsnNode(ASTORE, mcIndex));
		if(width != null && height != null) {
			insns.add(new VarInsnNode(ALOAD, mcIndex));
			insns.add(intInsn(launch.width.get()));
			insns.add(new FieldInsnNode(PUTFIELD, minecraft.name, width.name, width.desc));
			insns.add(new VarInsnNode(ALOAD, mcIndex));
			insns.add(intInsn(launch.height.get()));
			insns.add(new FieldInsnNode(PUTFIELD, minecraft.name, height.name, height.desc));
		}
		if(fullscreenField != null) {
			insns.add(new VarInsnNode(ALOAD, mcIndex));
			insns.add(booleanInsn(launch.fullscreen.get()));
			insns.add(new FieldInsnNode(PUTFIELD, minecraft.name, fullscreenField.name, fullscreenField.desc));
		}
		if(defaultWidth != null && defaultHeight != null) {
			insns.add(new VarInsnNode(ALOAD, mcIndex));
			insns.add(intInsn(launch.width.get()));
			insns.add(new FieldInsnNode(PUTFIELD, minecraft.name, defaultWidth.name, defaultWidth.desc));
			insns.add(new VarInsnNode(ALOAD, mcIndex));
			insns.add(intInsn(launch.height.get()));
			insns.add(new FieldInsnNode(PUTFIELD, minecraft.name, defaultHeight.name, defaultHeight.desc));
		}
		if(appletMode != null) {
    		insns.add(new VarInsnNode(ALOAD, mcIndex));
			insns.add(booleanInsn(launch.applet.get()));
    		insns.add(new FieldInsnNode(PUTFIELD, minecraft.name, appletMode.name, appletMode.desc));
		}
//		if(minecraftUri != null) {
//    		insns.add(new VarInsnNode(ALOAD, mcIndex));
//			insns.add(new LdcInsnNode("www.minecraft.net"));
//    		insns.add(new FieldInsnNode(PUTFIELD, minecraft.name, minecraftUri.name, minecraftUri.desc));
//		}
//		if(user != null) {
//			insns.add(new VarInsnNode(ALOAD, mcIndex));
//			insns.add(new TypeInsnNode(NEW, user.name));
//			insns.add(new InsnNode(DUP));
//			insns.add(new LdcInsnNode(launch.username.get()));
//			insns.add(new LdcInsnNode(launch.sessionid.get()));
//			insns.add(new MethodInsnNode(INVOKESPECIAL, user.name, "<init>", "(Ljava/lang/String;Ljava/lang/String;)V"));
//			insns.add(new FieldInsnNode(PUTFIELD, minecraft.name, userField.name, userField.desc));
//		}
//		if(hasPaid != null) {
//    		insns.add(new VarInsnNode(ALOAD, mcIndex));
//    		insns.add(new FieldInsnNode(GETFIELD, minecraft.name, userField.name, userField.desc));
//			insns.add(booleanInsn(launch.haspaid.get()));
//    		insns.add(new FieldInsnNode(PUTFIELD, hasPaid.owner, hasPaid.name, hasPaid.desc));
//		}
//		if(setDemo != null) {
//    		insns.add(new VarInsnNode(ALOAD, mcIndex));
//			insns.add(booleanInsn(launch.demo.get()));
//    		insns.add(new MethodInsnNode(INVOKEVIRTUAL, minecraft.name, setDemo.name, setDemo.desc));
//		}
//		if(setServer != null && launch.server.get() != null) {
//			insns.add(new VarInsnNode(ALOAD, mcIndex));
//    		insns.add(new LdcInsnNode(launch.server.get()));
//			insns.add(intInsn(launch.port.get()));
//			insns.add(new MethodInsnNode(INVOKEVIRTUAL, minecraft.name, setServer.name, setServer.desc));
//		}
		insns.add(new TypeInsnNode(NEW, "java/lang/Thread"));
		insns.add(new InsnNode(DUP));
		insns.add(new VarInsnNode(ALOAD, mcIndex));
		insns.add(new LdcInsnNode("Minecraft main thread"));
		insns.add(new MethodInsnNode(INVOKESPECIAL, "java/lang/Thread", "<init>", "(Ljava/lang/Runnable;Ljava/lang/String;)V"));
		insns.add(new VarInsnNode(ASTORE, threadIndex));
//		insns.add(new VarInsnNode(ALOAD, threadIndex));
//		insns.add(new IntInsnNode(BIPUSH, 10));
//		insns.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/Thread", "setPriority", "(I)V"));
		if(!launch.lwjglFrame.get()) {
			insns.add(new VarInsnNode(ALOAD, frameIndex));
			insns.add(new InsnNode(ICONST_1));
			insns.add(new MethodInsnNode(INVOKEVIRTUAL, "java/awt/Frame", "setVisible", "(Z)V"));
			insns.add(new VarInsnNode(ALOAD, frameIndex));
			insns.add(new TypeInsnNode(NEW, listenerClass));
			insns.add(new InsnNode(DUP));
			insns.add(new VarInsnNode(ALOAD, mcIndex));
			insns.add(new VarInsnNode(ALOAD, threadIndex));
			insns.add(new MethodInsnNode(INVOKESPECIAL, listenerClass, "<init>", "(L" + minecraft.name + ";Ljava/lang/Thread;)V"));
			insns.add(new MethodInsnNode(INVOKEVIRTUAL, "java/awt/Frame", "addWindowListener", "(Ljava/awt/event/WindowListener;)V"));
			createWindowListener(listenerClass);
		}
		insns.add(new VarInsnNode(ALOAD, threadIndex));
		insns.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/Thread", "start", "()V"));
		insns.add(new InsnNode(RETURN));
		debugInfo("Added main");
		return node;
	}
	
	private void patchAppletInit() {
		String mcField = null;
		String canvasField = null;
	    String mcDesc = "L" + minecraft.name + ";";
		MethodNode init = InjectUtils.getMethod(minecraftApplet, "init", "()V");
		if(init == null) {
			throw new IllegalStateException("Missing applet init");
		}
	    for(FieldNode field : minecraftApplet.fields) {
	    	if(mcDesc.equals(field.desc)) {
	    		field.access = Access.PUBLIC.setAccess(field.access);
	    		mcField = field.name;
	    	}
	    	if("Ljava/awt/Canvas;".equals(field.desc)) {
	    		canvasField = field.name;
	    	}
	    }
		AbstractInsnNode insn = init.instructions.getFirst();
		while(insn != null) {
			AbstractInsnNode[] insns2 = fill(insn, 5);
			if(compareInsn(insns2[1], PUTFIELD, minecraftApplet.name, mcField, mcDesc)
			&& compareInsn(insns2[0], INVOKESPECIAL, null, "<init>")) {
				MethodInsnNode invoke = (MethodInsnNode)insns2[0];
				init.instructions.insertBefore(insns2[1], getNewMinecraftImpl(source.getClass(invoke.owner), canvasField));
				IdentifyCall call = new IdentifyCall(invoke);
				AbstractInsnNode newInsn = call.getArgument(0)[0].getPrevious();
				if(newInsn.getOpcode() == NEW) {
					init.instructions.remove(newInsn);
				}
				for(AbstractInsnNode[] arg : call.getArguments()) {
					remove(init.instructions, arg);
				}
				init.instructions.remove(invoke);
				insn = insns2[1];
			}
			if(compareInsn(insns2[0], ALOAD, 0)
			&& compareInsn(insns2[1], GETFIELD, minecraftApplet.name, mcField, mcDesc)
			&& compareInsn(insns2[2], LDC, "79.136.77.240")
			&& compareInsn(insns2[3], SIPUSH)
			&& compareInsn(insns2[4], INVOKEVIRTUAL, minecraft.name, null, "(Ljava/lang/String;I)V")) {
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
	}

	private void createWindowListener(String listenerClass) {
		running.access = Access.PUBLIC.setAccess(running.access);
		
		ClassNode node = new ClassNode();
		node.visit(49, ACC_PUBLIC, listenerClass, null, "java/awt/event/WindowAdapter", null);
		node.fields.add(new FieldNode(ACC_PRIVATE, "mc", "L" + minecraft.name + ";", null, null));
		node.fields.add(new FieldNode(ACC_PRIVATE, "thread", "Ljava/lang/Thread;", null, null));
		MethodNode init = new MethodNode(ACC_PUBLIC, "<init>", "(L" + minecraft.name + ";Ljava/lang/Thread;)V", null, null);
		InsnList insns = init.instructions;
		insns.add(new VarInsnNode(ALOAD, 0));
		insns.add(new VarInsnNode(ALOAD, 1));
		insns.add(new FieldInsnNode(PUTFIELD, listenerClass, "mc", "L" + minecraft.name + ";"));
		insns.add(new VarInsnNode(ALOAD, 0));
		insns.add(new VarInsnNode(ALOAD, 2));
		insns.add(new FieldInsnNode(PUTFIELD, listenerClass, "thread", "Ljava/lang/Thread;"));
		insns.add(new VarInsnNode(ALOAD, 0));
		insns.add(new MethodInsnNode(INVOKESPECIAL, "java/awt/event/WindowAdapter", "<init>", "()V"));
	    insns.add(new InsnNode(RETURN));
	    node.methods.add(init);

		MethodNode windowClosing = new MethodNode(ACC_PUBLIC, "windowClosing", "(Ljava/awt/event/WindowEvent;)V", null, null);
		insns = windowClosing.instructions;

		LabelNode l0 = new LabelNode();
		LabelNode l1 = new LabelNode();
		LabelNode l2 = new LabelNode();
		LabelNode l4 = new LabelNode();
		windowClosing.tryCatchBlocks.add(new TryCatchBlockNode(l0, l1, l2, "java/lang/InterruptedException"));

		insns.add(new VarInsnNode(ALOAD, 0));
		insns.add(new FieldInsnNode(GETFIELD, listenerClass, "mc", "L" + minecraft.name + ";"));
		insns.add(new InsnNode(ICONST_0));
		insns.add(new FieldInsnNode(PUTFIELD, minecraft.name, running.name, running.desc));
	    insns.add(l0);
	    insns.add(new VarInsnNode(ALOAD, 0));
	    insns.add(new FieldInsnNode(GETFIELD, listenerClass, "thread", "Ljava/lang/Thread;"));
		insns.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/Thread", "join", "()V"));
	    insns.add(l1);
		insns.add(new JumpInsnNode(GOTO, l4));
	    insns.add(l2);
	    insns.add(new VarInsnNode(ASTORE, 2));
	    insns.add(new VarInsnNode(ALOAD, 2));
		insns.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/InterruptedException", "printStackTrace", "()V"));
	    insns.add(l4);
	    insns.add(new InsnNode(ICONST_0));
		insns.add(new MethodInsnNode(INVOKESTATIC, "java/lang/System", "exit", "(I)V"));
	    insns.add(new InsnNode(RETURN));
	    node.methods.add(windowClosing);
		source.overrideClass(node);
	}

	private InsnList getNewMinecraftImpl(ClassNode minecraftImpl, String canvasField) {
		MethodNode init = null;
		for(MethodNode m : minecraftImpl.methods) {
			if(m.name.equals("<init>")) {
				init = m;
			}
		}
		if(init == null) {
			throw new NullPointerException();
		}

//		final int appletIndex = 1;
//		final int frameIndex = 2;
//		final int canvasIndex = 3;

		InsnList insns = new InsnList();
		insns.add(new TypeInsnNode(NEW, minecraftImpl.name));
		insns.add(new InsnNode(DUP));
		
		Type[] types = Type.getArgumentTypes(init.desc);

		int i = 0;
		for(Type type : types) {
			String desc = type.getDescriptor();
			if(desc.equals("Ljava/awt/Canvas;")) {
				if(launch.lwjglFrame.get()) {
					insns.add(new InsnNode(ACONST_NULL));
				} else {
					//insns.add(new VarInsnNode(ALOAD, canvasIndex));
					insns.add(new VarInsnNode(ALOAD, 0));
					insns.add(new FieldInsnNode(GETFIELD, minecraftApplet.name, canvasField, "Ljava/awt/Canvas;"));
				}
			} else if(desc.equals("Ljava/awt/Component;")) {
				if(launch.lwjglFrame.get()) {
					insns.add(new InsnNode(ACONST_NULL));
				} else {
					//insns.add(new VarInsnNode(ALOAD, frameIndex));
				}
			} else if(minecraftApplet != null && desc.equals("L" + minecraftApplet.name + ";")) {
				//insns.add(new VarInsnNode(ALOAD, appletIndex));
				insns.add(new VarInsnNode(ALOAD, 0));
			}
			else if(desc.equals("I")) {
				if(i == 0) {
					insns.add(intInsn(launch.width.get()));
				} else if(i == 1) {
					insns.add(intInsn(launch.height.get()));
				} else {
					throw new IllegalStateException("Unexpected constructor");
				}
				i++;
			} else if(desc.equals("Z")) {
				insns.add(booleanInsn(launch.fullscreen.get()));
			} else {
				throw new IllegalStateException("Unexpected constructor");
			}
		}
		insns.add(new MethodInsnNode(INVOKESPECIAL, minecraftImpl.name, "<init>", init.desc));
		return insns;
	}
	
	private void fixSplash() {
		for(MethodNode m : minecraft.methods) {
			for(AbstractInsnNode insn : m.instructions) {
				if(insn.getOpcode() == LDC) {
					LdcInsnNode ldc = (LdcInsnNode)insn;
					if(ldc.cst.equals("/title/mojang.png")) {
						AbstractInsnNode insn3 = ldc.getNext();
						boolean store2 = false;
						boolean store3 = false;
						while(insn3 != null) {
							AbstractInsnNode[] insns2 = fill(insn3, 15);
							if(compareInsn(insns2[0], ISTORE, 2)) {
								store2 = true;
							}
							if(compareInsn(insns2[0], ISTORE, 3)) {
								store3 = true;
							}
							if(store2 && store3
							&& compareInsn(insns2[0], ALOAD)
							&& compareInsn(insns2[1], GETFIELD, null, null, "I")
							&& compareInsn(insns2[2], ICONST_2)
							&& compareInsn(insns2[3], IDIV)
							&& compareInsn(insns2[4], ILOAD)
							&& compareInsn(insns2[5], ISUB)
							&& compareInsn(insns2[6], ICONST_2)
							&& compareInsn(insns2[7], IDIV)
							&& compareInsn(insns2[8], ALOAD)
							&& compareInsn(insns2[9], GETFIELD, null, null, "I")
							&& compareInsn(insns2[10], ICONST_2)
							&& compareInsn(insns2[11], IDIV)
							&& compareInsn(insns2[12], ILOAD)
							&& compareInsn(insns2[13], ISUB)
							&& compareInsn(insns2[14], ICONST_2)) {
								debugInfo("Splash fix");
								m.instructions.remove(insns2[2]);
								m.instructions.remove(insns2[3]);
								m.instructions.remove(insns2[10]);
								m.instructions.remove(insns2[11]);
								m.instructions.remove(insns2[0]);
								m.instructions.remove(insns2[8]);
								m.instructions.set(insns2[1], new VarInsnNode(ILOAD, 2));
								m.instructions.set(insns2[9], new VarInsnNode(ILOAD, 3));
							}
							insn3 = nextInsn(insn3);
						}
						break;
					}
				}
			}
		}
	}
	
	private void optionsLoadFix(MethodNode init) {
		AbstractInsnNode insn = init.instructions.getFirst();
		while(insn != null) {
			AbstractInsnNode[] insns = fill(insn, 8);
			if(compareInsn(insns[0], ALOAD)
			&& compareInsn(insns[1], NEW)
			&& compareInsn(insns[2], DUP)
			&& compareInsn(insns[3], ALOAD)
			&& compareInsn(insns[4], ALOAD)
			&& compareInsn(insns[5], GETFIELD, minecraft.name, null, "Ljava/io/File;")
			&& compareInsn(insns[6], INVOKESPECIAL, null, "<init>")
			&& compareInsn(insns[7], PUTFIELD, minecraft.name)) {
				MethodInsnNode invoke = (MethodInsnNode)insns[6];
				ClassNode optionsClass = source.getClass(invoke.owner);
				if(optionsClass != null) {
					MethodNode optionsInit = InjectUtils.getMethod(optionsClass, invoke.name, invoke.desc);
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
								invoke2 = (MethodInsnNode)insn2;
							}
							insn2 = previousInsn(insn2);
						}
						if(isOptions && invoke2 != null) {
							MethodNode optionsInit2 = InjectUtils.getMethod(optionsClass, invoke2.name, invoke2.desc);
							
							for(TryCatchBlockNode tryCatch : optionsInit2.tryCatchBlocks) {
								AbstractInsnNode insn3 = nextInsn(tryCatch.start);
								AbstractInsnNode[] insns3 = fill(insn3, 4);
								if(compareInsn(insns3[0], ALOAD)
								&& compareInsn(insns3[1], LDC, ":")
								&& compareInsn(insns3[2], INVOKEVIRTUAL, "java/lang/String", "split", "(Ljava/lang/String;)[Ljava/lang/String;")
								&& compareInsn(insns3[3], ASTORE)) {
									return;
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
									var0 = (VarInsnNode)insns3[3];
								}
								
								if(compareInsn(insns3[0], ALOAD)
								&& compareInsn(insns3[1], LDC, ":")
								&& compareInsn(insns3[2], INVOKEVIRTUAL, "java/lang/String", "split", "(Ljava/lang/String;)[Ljava/lang/String;")) {
									if(lbl == null) {
										return;
									}
									if(compareInsn(insns3[3], DUP)
									&& compareInsn(insns3[4], ASTORE)) {
										VarInsnNode var1 = (VarInsnNode)insns3[4];
										if(var1.var == var0.var) {
											VarInsnNode aload = (VarInsnNode)insns3[0];
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
											tryCatchAdd(optionsInit2, insn3, insn4.getPrevious(), handle, "java/lang/Exception");
											source.overrideClass(optionsClass);
											debugInfo("Options load fix");
											return;
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
	}
	
	@Deprecated
	private void fixPaulscode() {
		ClassNode libraryOpenAL = source.getClass("paulscode/sound/libraries/LibraryLWJGLOpenAL");
		ClassNode libraryChannel = source.getClass("paulscode/sound/libraries/ChannelLWJGLOpenAL");
		if(libraryOpenAL == null || libraryChannel == null) {
			return;
		}
		MethodNode loadSound = InjectUtils.getMethod(libraryOpenAL, "loadSound", "(Lpaulscode/sound/FilenameURL;)Z");
		if(loadSound == null) {
			return;
		}
		MethodNode queueBuffer = InjectUtils.getMethod(libraryChannel, "queueBuffer", "([B)Z");
		if(queueBuffer == null) {
			return;
		}
		MethodNode preLoadBuffers = InjectUtils.getMethod(libraryChannel, "preLoadBuffers", "(Ljava/util/LinkedList;)Z");
		if(preLoadBuffers == null) {
			return;
		}
		MethodNode init = InjectUtils.getMethod(libraryChannel, "<init>", "(ILjava/nio/IntBuffer;)V");
		if(init == null) {
			return;
		}
		AbstractInsnNode insn = loadSound.instructions.getFirst();
		boolean success = false;
		while(insn != null) {
			AbstractInsnNode[] insns = fill(insn, 11);
			if(compareInsn(insns[0], ALOAD, 5)
			&& compareInsn(insns[1], ICONST_0)
			&& compareInsn(insns[2], INVOKEVIRTUAL, "java/nio/IntBuffer", "get", "(I)I")
			&& compareInsn(insns[3], ILOAD, 4)
			&& compareInsn(insns[4], ALOAD, 3)
			&& compareInsn(insns[5], GETFIELD, "paulscode/sound/SoundBuffer", "audioData", "[B")
			&& compareInsn(insns[6], INVOKESTATIC, "java/nio/ByteBuffer", "wrap", "([B)Ljava/nio/ByteBuffer;")
			&& compareInsn(insns[7], ALOAD, 2)
			&& compareInsn(insns[8], INVOKEVIRTUAL, "javax/sound/sampled/AudioFormat", "getSampleRate", "()F")
			&& compareInsn(insns[9], F2I)
			&& compareInsn(insns[10], INVOKESTATIC, "org/lwjgl/openal/AL10", "alBufferData", "(IILjava/nio/ByteBuffer;I)V")) {
				loadSound.instructions.remove(insns[5]);
				loadSound.instructions.remove(insns[6]);
				int freeIndex = getFreeIndex(loadSound.instructions);
				((VarInsnNode)insns[4]).var = freeIndex;
				InsnList insert = new InsnList();
				insert.add(new VarInsnNode(ALOAD, 3));
				insert.add(new FieldInsnNode(GETFIELD, "paulscode/sound/SoundBuffer", "audioData", "[B"));
				insert.add(new InsnNode(ARRAYLENGTH));
				insert.add(new MethodInsnNode(INVOKESTATIC, "org/lwjgl/BufferUtils", "createByteBuffer", "(I)Ljava/nio/ByteBuffer;"));
				insert.add(new InsnNode(DUP));
				insert.add(new VarInsnNode(ASTORE, freeIndex));
				insert.add(new MethodInsnNode(INVOKEVIRTUAL, "java/nio/ByteBuffer", "clear", "()Ljava/nio/Buffer;"));
				insert.add(new InsnNode(POP));
				insert.add(new VarInsnNode(ALOAD, freeIndex));
				insert.add(new VarInsnNode(ALOAD, 3));
				insert.add(new FieldInsnNode(GETFIELD, "paulscode/sound/SoundBuffer", "audioData", "[B"));
				insert.add(new MethodInsnNode(INVOKEVIRTUAL, "java/nio/ByteBuffer", "put", "([B)Ljava/nio/ByteBuffer;"));
				insert.add(new InsnNode(POP));
				insert.add(new VarInsnNode(ALOAD, freeIndex));
				insert.add(new MethodInsnNode(INVOKEVIRTUAL, "java/nio/ByteBuffer", "flip", "()Ljava/nio/Buffer;"));
				insert.add(new InsnNode(POP));
				loadSound.instructions.insertBefore(insn, insert);
				success |= true;
				break;
			}
			insn = nextInsn(insn);
		}
		boolean bufferBufferField = false;
		insn = queueBuffer.instructions.getFirst();
		while(insn != null) {
			AbstractInsnNode[] insns = fill(insn, 6);
			if(compareInsn(insns[0], ALOAD, 1)
			&& compareInsn(insns[1], ICONST_0)
			&& compareInsn(insns[2], ALOAD, 1)
			&& compareInsn(insns[3], ARRAYLENGTH)
			&& compareInsn(insns[4], INVOKESTATIC, "java/nio/ByteBuffer", "wrap", "([BII)Ljava/nio/ByteBuffer;")
			&& compareInsn(insns[5], ASTORE, 1)) {
				queueBuffer.instructions.remove(insns[4]);
				queueBuffer.instructions.remove(insns[5]);
				InsnList insert = new InsnList();
				insert.add(new VarInsnNode(ALOAD, 0));
				insert.add(new FieldInsnNode(GETFIELD, "paulscode/sound/libraries/ChannelLWJGLOpenAL", "bufferBuffer", "Ljava/nio/ByteBuffer;"));
				insert.add(new MethodInsnNode(INVOKEVIRTUAL, "java/nio/ByteBuffer", "clear", "()Ljava/nio/Buffer;"));
				insert.add(new InsnNode(POP));
				insert.add(new VarInsnNode(ALOAD, 0));
				insert.add(new FieldInsnNode(GETFIELD, "paulscode/sound/libraries/ChannelLWJGLOpenAL", "bufferBuffer", "Ljava/nio/ByteBuffer;"));
				queueBuffer.instructions.insertBefore(insn, insert);
				insert = new InsnList();
				insert.add(new MethodInsnNode(INVOKEVIRTUAL, "java/nio/ByteBuffer", "put", "([BII)Ljava/nio/ByteBuffer;"));
				insert.add(new InsnNode(POP));
				insert.add(new VarInsnNode(ALOAD, 0));
				insert.add(new FieldInsnNode(GETFIELD, "paulscode/sound/libraries/ChannelLWJGLOpenAL", "bufferBuffer", "Ljava/nio/ByteBuffer;"));
				insert.add(new MethodInsnNode(INVOKEVIRTUAL, "java/nio/ByteBuffer", "flip", "()Ljava/nio/Buffer;"));
				insert.add(new InsnNode(POP));
				insert.add(new VarInsnNode(ALOAD, 0));
				insert.add(new FieldInsnNode(GETFIELD, "paulscode/sound/libraries/ChannelLWJGLOpenAL", "bufferBuffer", "Ljava/nio/ByteBuffer;"));
				insert.add(new VarInsnNode(ASTORE, 1));
				queueBuffer.instructions.insert(insns[3], insert);
				bufferBufferField |= true;
				success |= true;
				break;
			}
			insn = nextInsn(insn);
		}
		insn = preLoadBuffers.instructions.getFirst();
		while(insn != null) {
			AbstractInsnNode[] insns = fill(insn, 12);
			if(compareInsn(insns[0], ALOAD, 1)
			&& compareInsn(insns[1], ILOAD, 4)
			&& compareInsn(insns[2], INVOKEVIRTUAL, "java/util/LinkedList", "get", "(I)Ljava/lang/Object;")
			&& compareInsn(insns[3], CHECKCAST, "[B")
			&& compareInsn(insns[4], ICONST_0)
			&& compareInsn(insns[5], ALOAD, 1)
			&& compareInsn(insns[6], ILOAD, 4)
			&& compareInsn(insns[7], INVOKEVIRTUAL, "java/util/LinkedList", "get", "(I)Ljava/lang/Object;")
			&& compareInsn(insns[8], CHECKCAST, "[B")
			&& compareInsn(insns[9], ARRAYLENGTH)
			&& compareInsn(insns[10], INVOKESTATIC, "java/nio/ByteBuffer", "wrap", "([BII)Ljava/nio/ByteBuffer;")
			&& compareInsn(insns[11], ASTORE, 3)) {
				preLoadBuffers.instructions.remove(insns[10]);
				preLoadBuffers.instructions.remove(insns[11]);
				InsnList insert = new InsnList();
				insert.add(new VarInsnNode(ALOAD, 0));
				insert.add(new FieldInsnNode(GETFIELD, "paulscode/sound/libraries/ChannelLWJGLOpenAL", "bufferBuffer", "Ljava/nio/ByteBuffer;"));
				insert.add(new MethodInsnNode(INVOKEVIRTUAL, "java/nio/ByteBuffer", "clear", "()Ljava/nio/Buffer;"));
				insert.add(new InsnNode(POP));
				insert.add(new VarInsnNode(ALOAD, 0));
				insert.add(new FieldInsnNode(GETFIELD, "paulscode/sound/libraries/ChannelLWJGLOpenAL", "bufferBuffer", "Ljava/nio/ByteBuffer;"));
				preLoadBuffers.instructions.insertBefore(insn, insert);
				insert = new InsnList();
				insert.add(new MethodInsnNode(INVOKEVIRTUAL, "java/nio/ByteBuffer", "put", "([BII)Ljava/nio/ByteBuffer;"));
				insert.add(new InsnNode(POP));
				insert.add(new VarInsnNode(ALOAD, 0));
				insert.add(new FieldInsnNode(GETFIELD, "paulscode/sound/libraries/ChannelLWJGLOpenAL", "bufferBuffer", "Ljava/nio/ByteBuffer;"));
				insert.add(new MethodInsnNode(INVOKEVIRTUAL, "java/nio/ByteBuffer", "flip", "()Ljava/nio/Buffer;"));
				insert.add(new InsnNode(POP));
				insert.add(new VarInsnNode(ALOAD, 0));
				insert.add(new FieldInsnNode(GETFIELD, "paulscode/sound/libraries/ChannelLWJGLOpenAL", "bufferBuffer", "Ljava/nio/ByteBuffer;"));
				insert.add(new VarInsnNode(ASTORE, 3));
				preLoadBuffers.instructions.insert(insns[9], insert);
				bufferBufferField |= true;
				success |= true;
				break;
			}
			insn = nextInsn(insn);
		}
		if(bufferBufferField) {
			if(InjectUtils.getField(libraryChannel, "bufferBuffer", "Ljava/nio/ByteBuffer;") == null) {
				libraryChannel.fields.add(new FieldNode(0, "bufferBuffer", "Ljava/nio/ByteBuffer;", null, null));
				insn = init.instructions.getFirst();
				while(insn != null) {
					if(compareInsn(insn, INVOKESPECIAL, "paulscode/sound/Channel", "<init>", "(I)V")) {
						InsnList insert = new InsnList();
						insert.add(new VarInsnNode(ALOAD, 0));
						insert.add(intInsn(0x500000));
						insert.add(new MethodInsnNode(INVOKESTATIC, "org/lwjgl/BufferUtils", "createByteBuffer", "(I)Ljava/nio/ByteBuffer;"));
						insert.add(new FieldInsnNode(PUTFIELD, "paulscode/sound/libraries/ChannelLWJGLOpenAL", "bufferBuffer", "Ljava/nio/ByteBuffer;"));
						init.instructions.insert(insn, insert);
						success |= true;
						break;
					}
					insn = nextInsn(insn);
				}
			}
		}
		if(success) {
			source.overrideClass(libraryChannel);
			source.overrideClass(libraryOpenAL);
			debugInfo("Fixed indirect buffers in paulscode");
		}
	}

	protected void init() {
		minecraftApplet = getApplet();
		minecraft = getMinecraft(minecraftApplet);
		if(minecraft == null) {
			return;
		}
	    for(MethodNode method : minecraft.methods) {
	    	if("main".equals(method.name) && "([Ljava/lang/String;)V".equals(method.desc) && (method.access & Opcodes.ACC_STATIC) != 0) {
	    		main = method;
	    	}
	    	if("run".equals(method.name) && "()V".equals(method.desc)) {
	    		AbstractInsnNode insn = method.instructions.getFirst();
	    		while(insn != null) {
	    			if(insn.getOpcode() == Opcodes.PUTFIELD) {
	    				FieldInsnNode putField = (FieldInsnNode)insn;
	    				if("Z".equals(putField.desc)) {
	    					running = InjectUtils.getField(minecraft, putField.name, putField.desc);
	    				}
						break;
	    			}
	    			insn = nextInsn(insn);
	    		}
	    	}
	    }
	    int i = 0;
	    for(FieldNode field : minecraft.fields) {
	    	if("I".equals(field.desc) && i <= 1) {
	    		// Width and height are always the first two ints in Minecraft class
				if(i == 0) width = field;
				if(i == 1) height = field;
	    		i++;
	    	}
	    	if("Ljava/io/File;".equals(field.desc)) {
	    		mcDir = field; // Possible candidate (Needed for infdev)
	    		if((field.access & Opcodes.ACC_STATIC) != 0) {
	    			// Definitely the mc directory
	    			break;
	    		}
	    	}
	    }
		if(minecraftApplet != null) {
		    String mcDesc = "L" + minecraft.name + ";";
		    String mcField = null;
		    for(FieldNode field : minecraftApplet.fields) {
		    	if(mcDesc.equals(field.desc)) {
		    		mcField = field.name;
		    	}
		    }
	
		    MethodNode init = InjectUtils.getMethod(minecraftApplet, "init", "()V");
	    	if(init != null) {
	    		AbstractInsnNode insn = init.instructions.getFirst();
	    		while(insn != null) {
	    			AbstractInsnNode[] insns = fillBackwards(insn, 8);
	    			if(compareInsn(insns[0], ALOAD)
	    			&& compareInsn(insns[1], GETFIELD, minecraftApplet.name, mcField, mcDesc)
	    			&& compareInsn(insns[2], ICONST_1)
	    			&& compareInsn(insns[3], PUTFIELD, minecraft.name, null, "Z")) {
    					FieldInsnNode field = (FieldInsnNode)insns[3];
    					appletMode = InjectUtils.getField(minecraft, field.name, field.desc);
	    			}
	    			if(compareInsn(insns[0], ALOAD)
	    			&& compareInsn(insns[1], GETFIELD, minecraftApplet.name, mcField, mcDesc)
	    			&& compareInsn(insns[2], LDC, "true")
	    			&& compareInsn(insns[3], ALOAD)
	    			&& compareInsn(insns[4], LDC, "stand-alone")
	    			&& compareInsn(insns[5], INVOKEVIRTUAL, minecraftApplet.name, "getParameter", "(Ljava/lang/String;)Ljava/lang/String;")
	    			&& compareInsn(insns[6], INVOKEVIRTUAL, "java/lang/String", "equals", "(Ljava/lang/Object;)Z")) {
	    				AbstractInsnNode[] insns2 = fill(insns[7], 7);
		    			if(compareInsn(insns2[0], IFNE)
				    	&& compareInsn(insns2[1], ICONST_1)
				    	&& compareInsn(insns2[2], GOTO)
				    	&& compareInsn(insns2[3], ICONST_0)
		    			&& compareInsn(insns2[4], PUTFIELD, minecraft.name, null, "Z")) {
	    					FieldInsnNode field = (FieldInsnNode)insns2[4];
	    					appletMode = InjectUtils.getField(minecraft, field.name, field.desc);
		    			}
	    			}
	    			insn = nextInsn(insn);
	    		}
	    	}
		}
	}

	public ClassNode getApplet() {
		ClassNode applet = null;
		for(String main : MAIN_APPLETS) {
			applet = source.getClass(main);
			if(applet != null) break;
		}
		return applet;
	}

	public ClassNode getMinecraft(ClassNode applet) {
		ClassNode launchTarget = null;
		for(String main : MAIN_CLASSES) {
			ClassNode cls = source.getClass(main);
			if(cls != null && cls.interfaces.contains("java/lang/Runnable")) {
				launchTarget = cls;
				break;
			}
		}
		if(launchTarget == null && applet != null) {
			for(FieldNode field : applet.fields) {
				String desc = field.desc;
				if(!desc.equals("Ljava/awt/Canvas;")
				&& !desc.equals("Ljava/lang/Thread;")
				&& desc.startsWith("L")
				&& desc.endsWith(";")) {
					launchTarget = source.getClass(desc.substring(1, desc.length() - 1));
				}
			}
		}
		return launchTarget;
	}
	
	public boolean isClassic() {
		for(String s : MAIN_CLASSES) {
			if(s.equals("net/minecraft/client/Minecraft")) {
				continue;
			}
			if(s.equals(minecraft.name)) {
				return true;
			}
		}
		if(minecraftApplet != null) {
			for(String s : MAIN_APPLETS) {
				if(s.equals("net/minecraft/client/MinecraftApplet")) {
					continue;
				}
				if(s.equals(minecraftApplet.name)) {
					return true;
				}
			}
		}
		return false;
	}
	
	private InsnList getGameDirectory() {
		InsnList insns = new InsnList();
		insns.add(new TypeInsnNode(NEW, "java/io/File"));
		insns.add(new InsnNode(DUP));
		insns.add(new LdcInsnNode(launch.gameDir.getString()));
		insns.add(new MethodInsnNode(INVOKESPECIAL, "java/io/File", "<init>", "(Ljava/lang/String;)V"));
		return insns;
	}
	
	public void replaceGameDirectory(MethodNode init, FieldNode mcDir) {
		InsnList insnList = init.instructions;
		AbstractInsnNode insn = insnList.getFirst();
		while(insn != null) {
			AbstractInsnNode[] insns = fill(insn, 6);
			// Indev game dir patch
			if(mcDir != null && launch.gameDir.get() != null
			&& compareInsn(insns[1], PUTFIELD, minecraft.name, mcDir.name, mcDir.desc)
			&& compareInsn(insns[0], ALOAD)) {
				insnList.remove(insns[0]);
				insnList.insertBefore(insns[1], getGameDirectory());
				insn = insns[1];
				debugInfo("Replaced gameDir");
			}
			// Classic game dir patch
			if(mcDir == null
			&& compareInsn(insns[0], ALOAD)
		    && compareInsn(insns[1], INVOKEVIRTUAL, "java/io/File", "exists", "()Z")
		    && compareInsn(insns[2], IFNE)
		    && compareInsn(insns[3], ALOAD)
		    && compareInsn(insns[4], INVOKEVIRTUAL, "java/io/File", "mkdirs", "()Z")
		    && compareInsn(insns[5], IFNE)) {
				LabelNode lbl = ((JumpInsnNode)insns[2]).label;
				int index = ((VarInsnNode)insns[0]).var;

				if(lbl == ((JumpInsnNode)insns[5]).label
				&& index == ((VarInsnNode)insns[3]).var) {
					AbstractInsnNode[] insns2 = fill(nextInsn(lbl), 2);
					if(compareInsn(insns2[0], ALOAD)
				    && compareInsn(insns2[1], ASTORE)) {
						if(index == ((VarInsnNode)insns2[0]).var) {
							insnList.remove(insns2[0]);
							insnList.insertBefore(insns2[1], getGameDirectory());
							debugInfo("Replaced gameDir");
						}
					}
				}
			}
			insn = nextInsn(insn);
		}
		if(mcDir != null && launch.gameDir.get() != null) {
			if(InjectUtils.isStatic(mcDir)) {
				MethodNode clinit = InjectUtils.getMethod(minecraft, "<clinit>", "()V");
				if(clinit != null) {
					InsnList insns = new InsnList();
					insns.add(getGameDirectory());
					insns.add(new FieldInsnNode(PUTSTATIC, minecraft.name, mcDir.name, mcDir.desc));
					clinit.instructions.insertBefore(getLastReturn(clinit.instructions.getLast()), insns);
    				debugInfo("Replaced gameDir");
				}
			} else {
				for(MethodNode m : minecraft.methods) {
					if(m.name.equals("<init>")) {
						InsnList insns = new InsnList();
						insns.add(new VarInsnNode(ALOAD, 0));
						insns.add(getGameDirectory());
						insns.add(new FieldInsnNode(PUTFIELD, minecraft.name, mcDir.name, mcDir.desc));
						m.instructions.insertBefore(getLastReturn(m.instructions.getLast()), insns);
        				debugInfo("Replaced gameDir");
					}
				}
			}
		}
	}

}
