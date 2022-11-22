package org.mcphackers.launchwrapper.tweak;

import static org.mcphackers.launchwrapper.inject.InsnHelper.*;
import static org.objectweb.asm.Opcodes.*;

import java.util.Arrays;

import org.mcphackers.launchwrapper.Launch;
import org.mcphackers.launchwrapper.LaunchTarget;
import org.mcphackers.launchwrapper.inject.ClassNodeSource;
import org.mcphackers.launchwrapper.inject.InjectUtils;
import org.mcphackers.launchwrapper.inject.InjectUtils.Access;
import org.mcphackers.launchwrapper.loader.LaunchClassLoader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

public class DefaultTweak extends Tweak {
	
	protected Launch launch;

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

	protected ClassNode minecraft;
	protected ClassNode minecraftApplet;
	/** Class containing username and sessionId */
	protected ClassNode user;
	/** Field of user class */
	protected FieldNode userField;
	/** Field that determines if Minecraft should exit */
	protected FieldNode running;
	/** Frame width */
	protected FieldNode width;
	/** Frame height */
	protected FieldNode height;
	/** Working game directory */
	protected FieldNode mcDir;
	/** public static main(String[]) */
	protected MethodNode main;
	/** Class used to instantiate a Minecraft instance */
	protected ClassNode minecraftImpl;
	
	public DefaultTweak(ClassNodeSource source, Launch launch) {
		super(source);
		this.launch = launch;
		init();
	}

	public boolean transform() {
		if(minecraft == null) {
			return false;
		}
		if(mcDir != null) {
			if(InjectUtils.isStatic(mcDir)) {
				for(MethodNode m : minecraft.methods) {
					if(m.name.equals("<clinit>") && m.desc.equals("()V")) {
						InsnList insns = new InsnList();
						insns.add(new TypeInsnNode(NEW, "java/io/File"));
						insns.add(new InsnNode(DUP));
						insns.add(new LdcInsnNode(launch.gameDir.getAbsolutePath()));
						insns.add(new MethodInsnNode(INVOKESPECIAL, "java/io/File", "<init>", "(Ljava/lang/String;)V"));
						insns.add(new FieldInsnNode(PUTSTATIC, minecraft.name, mcDir.name, mcDir.desc));
						m.instructions.insertBefore(getLastReturn(m.instructions.getLast()), insns);
						break;
					}
				}
			} else {
				for(MethodNode m : minecraft.methods) {
					if(m.name.equals("<init>")) {
						InsnList insns = new InsnList();
						insns.add(new VarInsnNode(ALOAD, 0));
						insns.add(new TypeInsnNode(NEW, "java/io/File"));
						insns.add(new InsnNode(DUP));
						insns.add(new LdcInsnNode(launch.gameDir.getAbsolutePath()));
						insns.add(new MethodInsnNode(INVOKESPECIAL, "java/io/File", "<init>", "(Ljava/lang/String;)V"));
						insns.add(new FieldInsnNode(PUTFIELD, minecraft.name, mcDir.name, mcDir.desc));
						m.instructions.insertBefore(getLastReturn(m.instructions.getLast()), insns);
					}
				}
			}
		}
		MethodNode constructor = null;
		if(width != null && height != null) {
			for(MethodNode m : minecraft.methods) {
				if(m.name.equals("<init>")) {
					constructor = m;
		    		AbstractInsnNode insn = m.instructions.getFirst();
		    		boolean widthReplaced = false;
		    		boolean heightReplaced = false;
		    		while(insn != null) {
		    			AbstractInsnNode[] insns = fill(insn, 3);
		    			if(compareInsn(insns[0], ALOAD)
		    			&& compareInsn(insns[2], PUTFIELD, minecraft.name, width.name, width.desc)) {
		    				m.instructions.set(insns[1], intInsn(launch.width));
		    				widthReplaced = true;
		    			}
		    			else if(compareInsn(insns[0], ALOAD)
		    			&& compareInsn(insns[2], PUTFIELD, minecraft.name, height.name, height.desc)) {
		    				m.instructions.set(insns[1], intInsn(launch.height));
		    				heightReplaced = true;
		    			}
		    			insn = insn.getNext();
		    		}
		    		if(!widthReplaced || !heightReplaced) {
						InsnList insns = new InsnList();
						if(!widthReplaced) {
							insns.add(new VarInsnNode(ALOAD, 0));
							insns.add(intInsn(launch.width));
							insns.add(new FieldInsnNode(PUTFIELD, minecraft.name, width.name, width.desc));
						}
						if(!heightReplaced) {
							insns.add(new VarInsnNode(ALOAD, 0));
							insns.add(intInsn(launch.height));
							insns.add(new FieldInsnNode(PUTFIELD, minecraft.name, height.name, height.desc));
						}
						m.instructions.insertBefore(getLastReturn(m.instructions.getLast()), insns);
		    		}
				}
			}
		}
		boolean hasInit = true;
    	MethodNode init = null;
    	MethodNode update = null;
    	MethodNode run = InjectUtils.getMethod(minecraft, "run", "()V");
    	if(run != null) {
    		AbstractInsnNode insn = run.instructions.getFirst();
    		while(insn != null) {
    			if(hasInit && insn.getType() == AbstractInsnNode.METHOD_INSN) {
    				hasInit = insn.getOpcode() == INVOKEVIRTUAL;
    			}
    			if(hasInit && init == null && insn.getOpcode() == INVOKEVIRTUAL) {
    				MethodInsnNode invoke = (MethodInsnNode)insn;
    				init = InjectUtils.getMethod(minecraft, invoke.name, invoke.desc);
    			}
    			AbstractInsnNode[] insns = fill(insn, 3);
    			if(compareInsn(insns[0], ALOAD)
				&& compareInsn(insns[1], GETFIELD, minecraft.name, running.name, running.desc)
	    		&& compareInsn(insns[2], IFEQ)) {
    	    		while(insn != null) {
    	    			if(compareInsn(insn.getPrevious(), ALOAD, 0)
    	    			&& compareInsn(insn, INVOKESPECIAL, minecraft.name, null, "()V")) {
	    	    			if(update == null) {
	    	    				MethodInsnNode invoke = (MethodInsnNode)insn;
	    	    				update = InjectUtils.getMethod(minecraft, invoke.name, invoke.desc);
	    	    			} else {
	    	    				break;
	    	    			}
    	    			}
    	    			insn = insn.getNext();
    	    		}
    				break;
    			}
				insn = insn.getNext();
    		}
    	} else {
    		return false;
    	}

    	for(InsnList insnList1 : update == null ? new InsnList[] {run.instructions} : new InsnList[] {run.instructions, update.instructions}) {
			AbstractInsnNode insn1 = insnList1.getFirst();
			while(insn1 != null) {
				AbstractInsnNode[] insns = fill(insn1, 3);
				if(compareInsn(insns[0], ALOAD)
				&& compareInsn(insns[1], GETFIELD, minecraft.name, null, "Ljava/awt/Canvas;")
	    		&& compareInsn(insns[2], INVOKEVIRTUAL, "java/awt/Canvas", "getWidth", "()I")) {
					MethodInsnNode invoke = new MethodInsnNode(INVOKESTATIC, "org/lwjgl/opengl/Display", "getWidth", "()I");
					insnList1.insert(insns[2], invoke);
					insnList1.remove(insns[0]);
					insnList1.remove(insns[1]);
					insnList1.remove(insns[2]);
					insn1 = invoke;
				}
				if(compareInsn(insns[0], ALOAD)
				&& compareInsn(insns[1], GETFIELD, minecraft.name, null, "Ljava/awt/Canvas;")
	    		&& compareInsn(insns[2], INVOKEVIRTUAL, "java/awt/Canvas", "getHeight", "()I")) {
					MethodInsnNode invoke = new MethodInsnNode(INVOKESTATIC, "org/lwjgl/opengl/Display", "getHeight", "()I");
					insnList1.insert(insns[2], invoke);
					insnList1.remove(insns[0]);
					insnList1.remove(insns[1]);
					insnList1.remove(insns[2]);
					insn1 = invoke;
				}
				insn1 = insn1.getNext();
	    	}
			insn1 = insnList1.getFirst();
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
					insnList1.remove(insns[0]);
					insnList1.remove(insns[1]);
					insnList1.remove(insns[2]);
					break;
				}
				insn1 = insn1.getNext();
			}
    	}
		AbstractInsnNode insn1 = run.instructions.getLast();
		while(insn1 != null && insn1.getOpcode() != ATHROW) {
			insn1 = insn1.getPrevious();
		}
		{
			AbstractInsnNode[] insns = fillBackwards(insn1, 4);
			if(compareInsn(insns[3], ATHROW)
			&& compareInsn(insns[1], INVOKEVIRTUAL, minecraft.name, null, "()V")
			&& compareInsn(insns[0], ALOAD)
			&& compareInsn(insns[2], ALOAD)) {
				MethodInsnNode invoke = (MethodInsnNode)insns[1];
				MethodNode stop = InjectUtils.getMethod(minecraft, invoke.name, invoke.desc);
				AbstractInsnNode insn2 = stop.instructions.getFirst();
				while(insn2 != null) {
					if(compareInsn(insn2, INVOKESTATIC, "org/lwjgl/opengl/Display", "destroy", "()V")) {
						AbstractInsnNode insn3 = insn2.getNext();
						while(insn3 != null && (insn3.getType() == AbstractInsnNode.LINE || insn3.getType() == AbstractInsnNode.LABEL)) {
							insn3 = insn3.getNext();
						}
						if(insn3 != null) {
							AbstractInsnNode[] insns2 = fill(insn3, 2);
							if(compareInsn(insns2[0], ICONST_0)
							&& compareInsn(insns2[1], INVOKESTATIC, "java/lang/System", "exit", "(I)V")) {
							} else {
								InsnList insert = new InsnList();
								insert.add(new InsnNode(ICONST_0));
								insert.add(new MethodInsnNode(INVOKESTATIC, "java/lang/System", "exit", "(I)V"));
								stop.instructions.insert(insn2, insert);
							}
						}
					}
					insn2 = insn2.getNext();
				}
			}
		}
		FieldInsnNode fullscreenField = null;
		String canvasName = null;
    	
    	{
    		int thisIndex = 0;
    		LabelNode aLabel = new LabelNode();
    		LabelNode iLabel = null;
    		LabelNode oLabel = null;
    		
    		AbstractInsnNode afterLabel = null;
    		
    		JumpInsnNode ifNoCanvas = null;
    		JumpInsnNode ifFullscreen = null;
    		
    		InsnList insnList = init == null ? run.instructions : init.instructions;
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
	    		}
    			// Any other pre-classic version
    			if(compareInsn(insns[0], NEW, "org/lwjgl/opengl/DisplayMode")
    			&& compareInsn(insns[1], DUP)
	    		&& compareInsn(insns[2], SIPUSH)
	    		&& compareInsn(insns[3], SIPUSH)
	    		&& compareInsn(insns[4], INVOKESPECIAL, "org/lwjgl/opengl/DisplayMode", "<init>", "(II)V")
	    		&& compareInsn(insns[5], INVOKESTATIC, "org/lwjgl/opengl/Display", "setDisplayMode", "(Lorg/lwjgl/opengl/DisplayMode;)V")) {
        			InsnList insert = getIcon();
    				if(!launch.fullscreen) {
	    				insnList.insert(insns[2], intInsn(launch.width));
	    				insnList.insert(insns[3], intInsn(launch.height));
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
        			InsnList insert = getIcon();
    				if(!launch.fullscreen) {
    					insert.add(new TypeInsnNode(NEW, "org/lwjgl/opengl/DisplayMode"));
    					insert.add(new InsnNode(DUP));
    					insert.add(intInsn(launch.width));
    					insert.add(intInsn(launch.height));
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
    	    			insn2 = insn2.getPrevious();
    	    		}
	    		}
	    		if(insn.getOpcode() == LDC) {
	    			LdcInsnNode ldc = (LdcInsnNode)insn;
	    			if(ldc.cst instanceof String) {
	    				String value = (String)ldc.cst;
	    				if(value.startsWith("Minecraft Minecraft")) {
	    					ldc.cst = value.substring(10);
	    				}
	    			}
	    		}
	    		if(canvasName != null && launch.lwjglFrame) {
	    			boolean found = compareInsn(insns[0], ALOAD, thisIndex)
		    	    			 && compareInsn(insns[1], GETFIELD, minecraft.name, canvasName, "Ljava/awt/Canvas;")
		    		    		 && compareInsn(insns[2], INVOKESPECIAL, null, "<init>", "(Ljava/awt/Component;)V");
	    			boolean found2 = found ? false :
	    							 compareInsn(insns[0], ALOAD, thisIndex)
		    	    			  && compareInsn(insns[1], GETFIELD, minecraft.name, canvasName, "Ljava/awt/Canvas;")
		    	    			  && compareInsn(insns[2], ALOAD, thisIndex)
		    	    			  && compareInsn(insns[3], GETFIELD, minecraft.name)
		    		    		  && compareInsn(insns[4], INVOKESPECIAL, null, "<init>");
	    			if(found || found2) {
	    				String owner = found ? ((MethodInsnNode)insns[2]).owner : ((MethodInsnNode)insns[4]).owner;
	    				ClassNode mouseHelper = source.getClass(owner);
	    				for(MethodNode m : mouseHelper.methods) {
	    					AbstractInsnNode insn2 = m.instructions.getFirst();
	    					while(insn2 != null) {
	    		    			AbstractInsnNode[] insns2 = fill(insn2, 4);

	    		    			if(compareInsn(insns2[0], ALOAD)
	    		    			&& compareInsn(insns2[1], GETFIELD, mouseHelper.name, null, "Ljava/awt/Component;")
	    		    			&& compareInsn(insns2[2], INVOKEVIRTUAL, "java/awt/Component", "getParent", "()Ljava/awt/Container;")
	    		    			&& compareInsn(insns2[3], IFNULL)) {
	    		    				LabelNode gotoLabel = ((JumpInsnNode)insns2[3]).label;
	    		    				m.instructions.insertBefore(insns2[0], new JumpInsnNode(GOTO, gotoLabel));
	    		    			}
	    		    			
	    		    			if(compareInsn(insns2[0], ALOAD)
	    		    			&& compareInsn(insns2[1], GETFIELD, mouseHelper.name, null, "Ljava/awt/Component;")
	    		        		&& compareInsn(insns2[2], INVOKEVIRTUAL, "java/awt/Component", "getWidth", "()I")) {
	    		    				MethodInsnNode invoke = new MethodInsnNode(INVOKESTATIC, "org/lwjgl/opengl/Display", "getWidth", "()I");
	    		    				m.instructions.insert(insns2[2], invoke);
	    		    				m.instructions.remove(insns2[0]);
	    		    				m.instructions.remove(insns2[1]);
	    		    				m.instructions.remove(insns2[2]);
	    		    				insn2 = invoke;
	    		    			}
	    		    			if(compareInsn(insns2[0], ALOAD)
	    		    			&& compareInsn(insns2[1], GETFIELD, mouseHelper.name, null, "Ljava/awt/Component;")
	    		        		&& compareInsn(insns2[2], INVOKEVIRTUAL, "java/awt/Component", "getHeight", "()I")) {
	    		    				MethodInsnNode invoke = new MethodInsnNode(INVOKESTATIC, "org/lwjgl/opengl/Display", "getHeight", "()I");
	    		    				m.instructions.insert(insns2[2], invoke);
	    		    				m.instructions.remove(insns2[0]);
	    		    				m.instructions.remove(insns2[1]);
	    		    				m.instructions.remove(insns2[2]);
	    		    				insn2 = invoke;
	    		    			}
	    		    			if(compareInsn(insns2[0], ALOAD)
	    		    			&& compareInsn(insns2[1], GETFIELD, mouseHelper.name, null, "Ljava/awt/Component;")
	    		        		&& compareInsn(insns2[2], INVOKEVIRTUAL, "java/awt/Component", "getLocationOnScreen", "()Ljava/awt/Point;")) {
	    		    				InsnList insert = new InsnList();
	    		    				insert.add(new TypeInsnNode(NEW, "java/awt/Point"));
	    		    				insert.add(new InsnNode(DUP));
	    		    				insert.add(new MethodInsnNode(INVOKESTATIC, "org/lwjgl/opengl/Display", "getX", "()I"));
	    		    				insert.add(new MethodInsnNode(INVOKESTATIC, "org/lwjgl/opengl/Display", "getY", "()I"));
	    		    				insert.add(new MethodInsnNode(INVOKESPECIAL, "java/awt/Point", "<init>", "(II)V"));
	    		    				insn2 = insert.getLast();
	    		    				m.instructions.insert(insns2[2], insert);
	    		    				m.instructions.remove(insns2[0]);
	    		    				m.instructions.remove(insns2[1]);
	    		    				m.instructions.remove(insns2[2]);
	    		    			}
	    						insn2 = insn2.getNext();
	    					}
	    				}
	    				source.overrideClass(mouseHelper);
	    			}
    			}
    			insn = insn.getNext();
    		}
    		
    		if(afterLabel != null
    	    && iLabel != null
    	    && oLabel != null
    	    && ifNoCanvas != null
    	    && ifFullscreen != null) {
    			insnList.insertBefore(afterLabel, aLabel);
    			InsnList insert = new InsnList();
    			insert.add(new InsnNode(ICONST_1));
    			insert.add(new TypeInsnNode(ANEWARRAY, "java/nio/ByteBuffer"));
    			insert.add(new InsnNode(DUP));
    			insert.add(new InsnNode(ICONST_0));
    			insert.add(new MethodInsnNode(INVOKESTATIC, "org/mcphackers/launchwrapper/Launch", "loadIcon", "()Ljava/nio/ByteBuffer;"));
    			insert.add(new InsnNode(AASTORE));
    			insert.add(new MethodInsnNode(INVOKESTATIC, "org/lwjgl/opengl/Display", "setIcon", "([Ljava/nio/ByteBuffer;)I"));
    			insert.add(new InsnNode(POP));
    			insert.add(new InsnNode(ICONST_1));
    			insert.add(new MethodInsnNode(INVOKESTATIC, "org/lwjgl/opengl/Display", "setResizable", "(Z)V"));
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
    	}
    	
    	if(fullscreenField != null) {
    		FieldNode defaultWidth = null;
    		FieldNode defaultHeight = null;
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
					insn2 = insn2.getNext();
				}
				while(insn2 != null) {
					AbstractInsnNode[] insns2 = fill(insn2, 4);
					if(compareInsn(insns2[0], INVOKESTATIC, "org/lwjgl/opengl/Display", "setFullscreen", "(Z)V")) {
	    				InsnList insert = new InsnList();
	    				// OpenGL is fucked up
	    				// setResizable returns early if the "previous" value of this flag is equal to the newly set one
	    				// Toggling fullscreen resets the window resizability but does not update the flag
	    				// Therefore it needs to be re-toggled to function properly
	        			insert.add(new InsnNode(ICONST_0));
	        			insert.add(new MethodInsnNode(INVOKESTATIC, "org/lwjgl/opengl/Display", "setResizable", "(Z)V"));
	        			insert.add(new InsnNode(ICONST_1));
	        			insert.add(new MethodInsnNode(INVOKESTATIC, "org/lwjgl/opengl/Display", "setResizable", "(Z)V"));
	    	    		m.instructions.insert(insns2[0], insert);
	    			}
	    			if(compareInsn(insns2[0], ALOAD)
	    			&& compareInsn(insns2[1], ALOAD)
					&& compareInsn(insns2[2], GETFIELD, minecraft.name, null, width.desc)
					&& compareInsn(insns2[3], PUTFIELD, minecraft.name, width.name, width.desc)) {
	    	    		defaultWidth = InjectUtils.getField(minecraft, ((FieldInsnNode)insns2[2]).name, width.desc);
	    			}
	    			if(compareInsn(insns2[0], ALOAD)
	    			&& compareInsn(insns2[1], ALOAD)
					&& compareInsn(insns2[2], GETFIELD, minecraft.name, null, height.desc)
					&& compareInsn(insns2[3], PUTFIELD, minecraft.name, height.name, height.desc)) {
	    	    		defaultHeight = InjectUtils.getField(minecraft, ((FieldInsnNode)insns2[2]).name, height.desc);
	    			}
	    			if(defaultWidth != null && defaultHeight != null
	    			&& compareInsn(insns2[0], IFGT)
	    			&& compareInsn(insns2[1], ALOAD)
					&& compareInsn(insns2[2], ICONST_1)
					&& compareInsn(insns2[3], PUTFIELD, minecraft.name, height.name, height.desc)) {
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
	    	    		m.instructions.insert(insns2[0], insert);
	    			}
					insn2 = insn2.getNext();
				}
    		}
    		AbstractInsnNode insn = constructor.instructions.getFirst();
    		boolean replaced = false;
    		while(insn != null) {
    			AbstractInsnNode[] insns = fill(insn, 3);
    			if(compareInsn(insns[0], ALOAD)
    			&& compareInsn(insns[2], PUTFIELD, minecraft.name, fullscreenField.name, fullscreenField.desc)) {
    				constructor.instructions.set(insns[1], booleanInsn(launch.fullscreen));
    				replaced = true;
    			}
    			insn = insn.getNext();
    		}
    		//TODO try to place it after super() call, not before return
    		if(!replaced) {
				InsnList insns = new InsnList();
				insns.add(new VarInsnNode(ALOAD, 0));
				insns.add(booleanInsn(launch.fullscreen));
				insns.add(new FieldInsnNode(PUTFIELD, minecraft.name, fullscreenField.name, fullscreenField.desc));
				constructor.instructions.insertBefore(getLastReturn(constructor.instructions.getLast()), insns);
    		}
    		if(defaultWidth != null && defaultHeight != null) {
				InsnList insns = new InsnList();
				insns.add(new VarInsnNode(ALOAD, 0));
				insns.add(intInsn(launch.width));
				insns.add(new FieldInsnNode(PUTFIELD, minecraft.name, defaultWidth.name, defaultWidth.desc));
				insns.add(new VarInsnNode(ALOAD, 0));
				insns.add(intInsn(launch.height));
				insns.add(new FieldInsnNode(PUTFIELD, minecraft.name, defaultHeight.name, defaultHeight.desc));
				constructor.instructions.insertBefore(getLastReturn(constructor.instructions.getLast()), insns);
    		}
    	}
    		
		final boolean replaceMain = true;
		if(main == null) {
			minecraft.methods.add(main = getMain());
		} else if(replaceMain) {
			minecraft.methods.remove(main);
			minecraft.methods.add(main = getMain());
		} else {
			//TODO inject into main
		}
		
		source.overrideClass(minecraft);
		return true;
	}

	private InsnList getIcon() {
		InsnList insert = new InsnList();
		insert.add(new InsnNode(ICONST_1));
		insert.add(new TypeInsnNode(ANEWARRAY, "java/nio/ByteBuffer"));
		insert.add(new InsnNode(DUP));
		insert.add(new InsnNode(ICONST_0));
		insert.add(new MethodInsnNode(INVOKESTATIC, "org/mcphackers/launchwrapper/Launch", "loadIcon", "()Ljava/nio/ByteBuffer;"));
		insert.add(new InsnNode(AASTORE));
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

		if(minecraftApplet != null) {
			insns.add(new TypeInsnNode(NEW, minecraftApplet.name));
			insns.add(new InsnNode(DUP));
			insns.add(new MethodInsnNode(INVOKESPECIAL, minecraftApplet.name, "<init>", "()V"));
			insns.add(new VarInsnNode(ASTORE, appletIndex));
			insns.add(new VarInsnNode(ALOAD, appletIndex));
			insns.add(new TypeInsnNode(NEW, "org/mcphackers/launchwrapper/inject/AppletWrapper"));
			insns.add(new InsnNode(DUP));
			insns.add(new MethodInsnNode(INVOKESPECIAL, "org/mcphackers/launchwrapper/inject/AppletWrapper", "<init>", "()V"));
			insns.add(new MethodInsnNode(INVOKEVIRTUAL, "java/applet/Applet", "setStub", "(Ljava/applet/AppletStub;)V"));
		}
		if(!launch.lwjglFrame) {
			insns.add(new TypeInsnNode(NEW, "java/awt/Frame"));
			insns.add(new InsnNode(DUP));
			insns.add(new LdcInsnNode("Minecraft"));
			insns.add(new MethodInsnNode(INVOKESPECIAL, "java/awt/Frame", "<init>", "(Ljava/lang/String;)V"));
			insns.add(new VarInsnNode(ASTORE, frameIndex));
			
			insns.add(new VarInsnNode(ALOAD, frameIndex));
			insns.add(new FieldInsnNode(GETSTATIC, "org/mcphackers/launchwrapper/Launch", "ICON", "Ljava/awt/image/BufferedImage;"));
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
			insns.add(intInsn(launch.width));
			insns.add(intInsn(launch.height));
			insns.add(new MethodInsnNode(INVOKESPECIAL, "java/awt/Dimension", "<init>", "(II)V"));
			insns.add(new MethodInsnNode(INVOKEVIRTUAL, "java/awt/Canvas", "setPreferredSize", "(Ljava/awt/Dimension;)V"));
			insns.add(new VarInsnNode(ALOAD, frameIndex));
			insns.add(new MethodInsnNode(INVOKEVIRTUAL, "java/awt/Frame", "pack", "()V"));
			insns.add(new VarInsnNode(ALOAD, frameIndex));
			insns.add(new InsnNode(ACONST_NULL));
			insns.add(new MethodInsnNode(INVOKEVIRTUAL, "java/awt/Frame", "setLocationRelativeTo", "(Ljava/awt/Component;)V"));
		}
		insns.add(getMinecraftImpl());
		insns.add(new VarInsnNode(ASTORE, mcIndex));
		insns.add(new TypeInsnNode(NEW, "java/lang/Thread"));
		insns.add(new InsnNode(DUP));
		insns.add(new VarInsnNode(ALOAD, mcIndex));
		insns.add(new LdcInsnNode("Minecraft main thread"));
		insns.add(new MethodInsnNode(INVOKESPECIAL, "java/lang/Thread", "<init>", "(Ljava/lang/Runnable;Ljava/lang/String;)V"));
		insns.add(new VarInsnNode(ASTORE, threadIndex));
		insns.add(new VarInsnNode(ALOAD, threadIndex));
		insns.add(new IntInsnNode(BIPUSH, 10));
		insns.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/Thread", "setPriority", "(I)V"));
		if(user != null) {
			insns.add(new VarInsnNode(ALOAD, mcIndex));
			insns.add(new TypeInsnNode(NEW, user.name));
			insns.add(new InsnNode(DUP));
			insns.add(new LdcInsnNode(launch.username));
			insns.add(new LdcInsnNode(launch.sessionId));
			insns.add(new MethodInsnNode(INVOKESPECIAL, user.name, "<init>", "(Ljava/lang/String;Ljava/lang/String;)V"));
			insns.add(new FieldInsnNode(PUTFIELD, minecraft.name, userField.name, userField.desc));
		}
		if(!launch.lwjglFrame) {
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
		return node;
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

	private InsnList getMinecraftImpl() {
		MethodNode init = null;
		for(MethodNode m : minecraftImpl.methods) {
			if(m.name.equals("<init>")) {
				init = m;
			}
		}
		if(init == null) {
			throw new NullPointerException();
		}

		final int appletIndex = 1;
		final int frameIndex = 2;
		final int canvasIndex = 3;

		InsnList insns = new InsnList();
		insns.add(new TypeInsnNode(NEW, minecraftImpl.name));
		insns.add(new InsnNode(DUP));
		
		Type[] types = Type.getArgumentTypes(init.desc);

		if(types.length == 0) {
			// Prevent any further checks
		}
		else if(Arrays.equals(types, new Type[] { Type.getType("Ljava/awt/Canvas;"), Type.getType("I"), Type.getType("I"), Type.getType("Z") })) {
			if(launch.lwjglFrame) {
				insns.add(new InsnNode(ACONST_NULL));
			} else {
				insns.add(new VarInsnNode(ALOAD, canvasIndex));
			}
			insns.add(intInsn(launch.width));
			insns.add(intInsn(launch.height));
			insns.add(booleanInsn(launch.fullscreen));
		}
		else if(minecraftApplet == null) {
			throw new NullPointerException("minecraftApplet is null");
		}
		else if(Arrays.equals(types, new Type[] { Type.getType("Ljava/awt/Canvas;"), Type.getType("L" + minecraftApplet.name + ";"), Type.getType("I"), Type.getType("I"), Type.getType("Z") })) {
			
			if(launch.lwjglFrame) {
				insns.add(new InsnNode(ACONST_NULL));
			} else {
				insns.add(new VarInsnNode(ALOAD, canvasIndex));
			}
			insns.add(new VarInsnNode(ALOAD, appletIndex));
			insns.add(intInsn(launch.width));
			insns.add(intInsn(launch.height));
			insns.add(booleanInsn(launch.fullscreen));
		}
		else if(Arrays.equals(types, new Type[] { Type.getType("Ljava/awt/Component;"), Type.getType("Ljava/awt/Canvas;"), Type.getType("L" + minecraftApplet.name + ";"), Type.getType("I"), Type.getType("I"), Type.getType("Z") })) {
			if(launch.lwjglFrame) {
				insns.add(new InsnNode(ACONST_NULL));
				insns.add(new InsnNode(ACONST_NULL));
			} else {
				insns.add(new VarInsnNode(ALOAD, frameIndex));
				insns.add(new VarInsnNode(ALOAD, canvasIndex));
			}
			insns.add(new VarInsnNode(ALOAD, appletIndex));
			insns.add(intInsn(launch.width));
			insns.add(intInsn(launch.height));
			insns.add(booleanInsn(launch.fullscreen));
		}
		else if(Arrays.equals(types, new Type[] { Type.getType("L" + minecraftApplet.name + ";"), Type.getType("Ljava/awt/Canvas;"), Type.getType("L" + minecraftApplet.name + ";"), Type.getType("I"), Type.getType("I"), Type.getType("Z") })) {
			insns.add(new VarInsnNode(ALOAD, appletIndex));
			if(launch.lwjglFrame) {
				insns.add(new InsnNode(ACONST_NULL));
			} else {
				insns.add(new VarInsnNode(ALOAD, canvasIndex));
			}
			insns.add(new VarInsnNode(ALOAD, appletIndex));
			insns.add(intInsn(launch.width));
			insns.add(intInsn(launch.height));
			insns.add(booleanInsn(launch.fullscreen));
		}
		else if(Arrays.equals(types, new Type[] { Type.getType("L" + minecraftApplet.name + ";"), Type.getType("Ljava/awt/Component;"), Type.getType("Ljava/awt/Canvas;"), Type.getType("L" + minecraftApplet.name + ";"), Type.getType("I"), Type.getType("I"), Type.getType("Z") })) {
			insns.add(new VarInsnNode(ALOAD, appletIndex));
			if(launch.lwjglFrame) {
				insns.add(new InsnNode(ACONST_NULL));
				insns.add(new InsnNode(ACONST_NULL));
			} else {
				insns.add(new VarInsnNode(ALOAD, frameIndex));
				insns.add(new VarInsnNode(ALOAD, canvasIndex));
			}
			insns.add(new VarInsnNode(ALOAD, appletIndex));
			insns.add(intInsn(launch.width));
			insns.add(intInsn(launch.height));
			insns.add(booleanInsn(launch.fullscreen));
		}
		insns.add(new MethodInsnNode(INVOKESPECIAL, minecraftImpl.name, "<init>", init.desc));
		return insns;
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
	    			insn = insn.getNext();
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
	
		    for(MethodNode method : minecraftApplet.methods) {
		    	if("init".equals(method.name) && "()V".equals(method.desc)) {
		    		AbstractInsnNode insn = method.instructions.getFirst();
		    		while(insn != null) {
		    			AbstractInsnNode[] insns = fillBackwards(insn, 2);
		    			if(compareInsn(insns[1], PUTFIELD, minecraftApplet.name, mcField, mcDesc)
	    				&& compareInsn(insns[0], INVOKESPECIAL, null, "<init>")) {
		    				MethodInsnNode invoke = (MethodInsnNode)insns[0];
							minecraftImpl = source.getClass(invoke.owner);
		    			}
	    				
		    			insns = fill(insn, 8);
		    			if(compareInsn(insns[0], ALOAD)
	    				&& compareInsn(insns[1], LDC, "username")
	    				&& compareInsn(insns[2], INVOKEVIRTUAL, minecraftApplet.name, "getParameter", "(Ljava/lang/String;)Ljava/lang/String;")
	    				&& compareInsn(insns[3], ALOAD)
	    				&& compareInsn(insns[4], LDC, "sessionid")
	    				&& compareInsn(insns[5], INVOKEVIRTUAL, minecraftApplet.name, "getParameter", "(Ljava/lang/String;)Ljava/lang/String;")
	    				&& compareInsn(insns[6], INVOKESPECIAL, null, "<init>", "(Ljava/lang/String;Ljava/lang/String;)V")
		    			) {
		    				MethodInsnNode invokespecial = (MethodInsnNode)insns[6];
	    					user = source.getClass(invokespecial.owner);
		    				if(compareInsn(insns[7], PUTFIELD, minecraft.name)) {
		    					FieldInsnNode field = (FieldInsnNode)insns[7];
    	    					userField = InjectUtils.getField(minecraft, field.name, field.desc);
		    				}
		    			}
		    			if(compareInsn(insns[0], GETFIELD, minecraftApplet.name, mcField, mcDesc)
	    				&& compareInsn(insns[1], NEW)
						&& compareInsn(insns[2], DUP)
						&& compareInsn(insns[3], INVOKESPECIAL, null, "<init>")) {
		    				if(compareInsn(insns[4], PUTFIELD, minecraft.name)) {
		    					FieldInsnNode field = (FieldInsnNode)insns[4];
		    					if(field.desc.startsWith("L") && field.desc.endsWith(";")) {
			    					user = source.getClass(field.desc.substring(1, field.desc.length() - 1));
	    	    					userField = InjectUtils.getField(minecraft, field.name, field.desc);
		    					}
		    				}
		    			}
		    			insn = insn.getNext();
		    		}
		    	}
		    }
		}
		if(minecraftImpl == null) {
			minecraftImpl = minecraft;
		}
	}
	
	public LaunchTarget getLaunchTarget() {
		if(main != null) {
			return new LaunchTarget(minecraft.name, LaunchTarget.Type.MAIN);
		}
		return null;
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

	public boolean supportsCanvas() {
		for(MethodNode m : InjectUtils.getConstructors(minecraftImpl)) {
			if(Type.getArgumentTypes(m.desc).length > 0) {
				return true;
			}
		}
		return false;
	}
	
	public static Tweak get(LaunchClassLoader classLoader, Launch launch) {
		if(launch.isom) {
			return new IsomTweak(classLoader, launch);
		}
		return new DefaultTweak(classLoader, launch);
	}

}
