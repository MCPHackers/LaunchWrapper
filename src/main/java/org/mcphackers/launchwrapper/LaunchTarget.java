package org.mcphackers.launchwrapper;

import static org.mcphackers.launchwrapper.InsnHelper.*;
import static org.objectweb.asm.Opcodes.*;

import java.applet.Applet;
import java.awt.Canvas;
import java.awt.Component;
import java.awt.Frame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

public class LaunchTarget {
	public final Class<?> minecraft;
	public final Class<? extends Applet> minecraftApplet;
	public Constructor<?> session;
	public Field sessionField;
	public Field running;
	public Field width;
	public Field height;
	public Field mcDir;
	public Field user;
	public Method main;
	private Constructor<?> mcConstruct;
	
	public LaunchTarget(Class<?> mc, Class<? extends Applet> mcApplet) throws IOException {
		minecraft = mc;
		minecraftApplet = mcApplet;
        readClasses();
	}
	
	private Field getDeclaredField(Class<?> owner, String name) {
		Field field = null;
		try {
			field = owner.getDeclaredField(name);
			if(!field.isAccessible()) {
				field.setAccessible(true);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return field;
	}
	
	private void readClasses() throws IOException {
		ClassNode classNode = new ClassNode();
	    ClassReader classReader = new ClassReader(minecraft.getName());
	    classReader.accept(classNode, ClassReader.EXPAND_FRAMES);
        try {
        	main = minecraft.getMethod("main", new Class[] { String[].class });
		} catch (Exception e) {
			// No main method
		}
	    for(MethodNode method : classNode.methods) {
	    	if("run".equals(method.name) && "()V".equals(method.desc)) {
	    		AbstractInsnNode insn = method.instructions.getFirst();
	    		while(insn != null) {
	    			if(insn.getOpcode() == Opcodes.PUTFIELD) {
	    				FieldInsnNode putField = (FieldInsnNode)insn;
	    				if("Z".equals(putField.desc)) {
	    					running = getDeclaredField(minecraft, putField.name);
	    				}
						break;
	    			}
	    			insn = insn.getNext();
	    		}
	    	}
	    }
	    FieldNode mcDirectory = null;
	    int i = 0;
	    for(FieldNode field : classNode.fields) {
	    	if("I".equals(field.desc) && i <= 1) {
	    		// Width and height are always the first two ints in Minecraft class
				if(i == 0) width = getDeclaredField(minecraft, field.name);
				if(i == 1) height = getDeclaredField(minecraft, field.name);
	    		i++;
	    	}
	    	if("Ljava/io/File;".equals(field.desc)) {
	    		mcDirectory = field; // Possible candidate (Needed for infdev)
	    		if((field.access & Opcodes.ACC_STATIC) != 0) {
	    			// Definitely the mc directory
	    			break;
	    		}
	    	}
	    }
	    if(mcDirectory != null) {
	    	mcDir = getDeclaredField(minecraft, mcDirectory.name);
	    }
		Class<?> minecraftImpl = minecraft;
		if(minecraftApplet != null) {
			classNode = new ClassNode();
		    classReader = new ClassReader(minecraftApplet.getName());
		    classReader.accept(classNode, ClassReader.EXPAND_FRAMES);
		    String mcAppletName = minecraftApplet.getName().replace('.', '/');
		    String mcName = minecraft.getName().replace('.', '/');
		    String mcDesc = "L" + mcName + ";";
		    String mcField = null;
		    for(FieldNode field : classNode.fields) {
		    	if(mcDesc.equals(field.desc)) {
		    		mcField = field.name;
		    	}
		    }
	
		    for(MethodNode method : classNode.methods) {
		    	if("init".equals(method.name) && "()V".equals(method.desc)) {
		    		AbstractInsnNode insn = method.instructions.getFirst();
		    		while(insn != null) {
		    			AbstractInsnNode[] insns = fillBackwards(insn, 2);
		    			if(compareInsn(insns[1], PUTFIELD, mcAppletName, mcField, mcDesc)
	    				&& compareInsn(insns[0], INVOKESPECIAL, null, "<init>")) {
		    				MethodInsnNode invoke = (MethodInsnNode)insns[0];
		    				try {
								minecraftImpl = Launch.CLASS_LOADER.findClass(invoke.owner.replace('/', '.'));
							} catch (ClassNotFoundException e) {
								minecraftImpl = null;
								e.printStackTrace();
							}
		    			}
	    				
	    				
		    			insns = fill(insn, 8);
		    			if(compareInsn(insns[0], ALOAD)
	    				&& compareInsn(insns[1], LDC, "username")
	    				&& compareInsn(insns[2], INVOKEVIRTUAL, mcAppletName, "getParameter", "(Ljava/lang/String;)Ljava/lang/String;")
	    				&& compareInsn(insns[3], ALOAD)
	    				&& compareInsn(insns[4], LDC, "sessionid")
	    				&& compareInsn(insns[5], INVOKEVIRTUAL, mcAppletName, "getParameter", "(Ljava/lang/String;)Ljava/lang/String;")
	    				&& compareInsn(insns[6], INVOKESPECIAL, null, "<init>", "(Ljava/lang/String;Ljava/lang/String;)V")
		    			) {
		    				MethodInsnNode invokespecial = (MethodInsnNode)insns[6];
    	    				try {
    	    					session = Launch.CLASS_LOADER.findClass(invokespecial.owner.replace('/', '.')).getConstructor(String.class, String.class);
							} catch (Exception e) {
								e.printStackTrace();
							}
		    				if(compareInsn(insns[7], PUTFIELD, mcName)) {
    	    					sessionField = getDeclaredField(minecraft, ((FieldInsnNode)insns[7]).name);
		    				}
		    			}
		    			insn = insn.getNext();
		    		}
		    	}
		    }
		}
	    
		Constructor<?>[] constructors = minecraftImpl.getConstructors();
		if(constructors.length >= 1) {
			mcConstruct = constructors[0];
		}
		if(constructors.length > 1) {
			System.out.println("WARNING: Main class contains more than one constructor!");
		}
		
	}

	public Applet getAppletInstance() {
		if(minecraftApplet == null) return null;
		try {
			Applet appletInstance = minecraftApplet.newInstance();
			return appletInstance;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public boolean supportsCanvas() {
		return mcConstruct.getParameterTypes().length != 0;
	}

	public Runnable getInstance(Frame frame, Canvas canvas, Applet applet, int w, int h, boolean fullscreen, boolean appletMode) {
		Class<?>[] types = mcConstruct.getParameterTypes();
		try {
			if(types.length == 0) {
				return (Runnable)mcConstruct.newInstance();
			}
			if(Arrays.equals(types, new Class[] { Canvas.class, int.class, int.class, boolean.class })) {
				return (Runnable)mcConstruct.newInstance(canvas, w, h, fullscreen);
			}
			if(Arrays.equals(types, new Class[] { Canvas.class, minecraftApplet, int.class, int.class, boolean.class })) {
				return (Runnable)mcConstruct.newInstance(canvas, applet, w, h, fullscreen);
			}
			if(Arrays.equals(types, new Class[] { Component.class, Canvas.class, minecraftApplet, int.class, int.class, boolean.class })) {
				return (Runnable)mcConstruct.newInstance(frame, canvas, applet, w, h, fullscreen);
			}
			if(Arrays.equals(types, new Class[] { minecraftApplet, Canvas.class, minecraftApplet, int.class, int.class, boolean.class })) {
				return (Runnable)mcConstruct.newInstance(applet, canvas, applet, w, h, fullscreen);
			}
			if(Arrays.equals(types, new Class[] { minecraftApplet, Component.class, Canvas.class, minecraftApplet, int.class, int.class, boolean.class })) {
				return (Runnable)mcConstruct.newInstance(applet, frame, canvas, applet, w, h, fullscreen);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public WindowListener getWindowListener(final Runnable mc, final Thread thread) {
		return new WindowAdapter() {
			public void windowClosing(WindowEvent event) {
				if(running != null) {
					Util.setField(mc, running, false);
					try {
						thread.join();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				System.exit(0);
			}
		};
	}

	public void setAppletMode(Object instance, boolean b) {
		Util.setField(instance, null, b); //FIXME
	}

	public void setUser(Object instance, String username, String sessionId) {
		if(session != null && sessionField != null) {
			try {
				Util.setField(instance, sessionField, session.newInstance(username, sessionId));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public void setServer(Object instance, String serverIp, int serverPort) {
		// TODO
	}
}
