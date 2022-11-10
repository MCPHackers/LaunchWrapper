package org.mcphackers.launchwrapper;

import java.applet.Applet;
import java.awt.Canvas;
import java.awt.Component;
import java.awt.Frame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.LdcInsnNode;
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
	
	private void readClasses() throws IOException {
		ClassNode classNode = new ClassNode();
	    ClassReader classReader = new ClassReader(minecraft.getName());
	    classReader.accept(classNode, ClassReader.EXPAND_FRAMES);
        try {
        	main = minecraft.getMethod("main", new Class[] { String[].class });
		} catch (Exception e) {
			// No main method
		}
        //MethodNode init = null;
	    for(MethodNode method : classNode.methods) {
	    	if("run".equals(method.name) && "()V".equals(method.desc)) {
	    		AbstractInsnNode insn = method.instructions.getFirst();
	    		while(insn != null) {
	    			if(insn.getOpcode() == Opcodes.PUTFIELD) {
	    				FieldInsnNode putField = (FieldInsnNode)insn;
	    				if("Z".equals(putField.desc)) {
	    					try {
								running = minecraft.getDeclaredField(putField.name);
								if(!running.isAccessible()) {
									running.setAccessible(true);
								}
							} catch (Exception e) {
								e.printStackTrace();
							}
	    				} else {
	    					// Stop looking for it, it's hopeless
	    					break;
	    				}
	    			}
	    			insn = insn.getNext();
	    		}
	    	}
	    	if("<init>".equals(method.name)) {
	    		//init = method; TODO
	    	}
	    }
	    FieldNode mcDirectory = null;
	    for(FieldNode field : classNode.fields) {
	    	if(Type.getDescriptor(File.class).equals(field.desc)) {
	    		mcDirectory = field; // Possible candidate (Needed for infdev)
	    		if((field.access & Opcodes.ACC_STATIC) != 0) {
	    			// Definitely the mc directory
	    			break;
	    		}
	    	}
	    }
	    if(mcDirectory != null) {
			try {
				mcDir = minecraft.getDeclaredField(mcDirectory.name);
				if(!mcDir.isAccessible()) {
					mcDir.setAccessible(true);
				}
			} catch (Exception e) {}
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
		    			if(insn.getOpcode() == Opcodes.PUTFIELD) {
		    				FieldInsnNode putField = (FieldInsnNode)insn;
		    				if(mcDesc.equals(putField.desc) && mcAppletName.equals(putField.owner) && putField.name.equals(mcField)) {
		    					if(putField.getPrevious().getOpcode() == Opcodes.INVOKESPECIAL) {
		    	    				MethodInsnNode invoke = (MethodInsnNode)putField.getPrevious();
		    	    				if("<init>".equals(invoke.name)) {
			    	    				try {
											minecraftImpl = Launch.CLASS_LOADER.loadClass(invoke.owner.replace('/', '.'));
										} catch (ClassNotFoundException e) {
											minecraftImpl = null;
											e.printStackTrace();
										}
		    	    				}
		    					}
		    				}
		    			}
		    			AbstractInsnNode insn2 = insn;
		    			if(insn2.getOpcode() == Opcodes.ALOAD) {
		    				insn2 = insn2.getNext();
			    			if(insn2 != null && insn2.getOpcode() == Opcodes.LDC && ((LdcInsnNode)insn2).cst.equals("username")) {
			    				insn2 = insn2.getNext();
				    			if(insn2 != null && insn2.getOpcode() == Opcodes.INVOKEVIRTUAL) {
				    				MethodInsnNode invoke = (MethodInsnNode)insn2;
				    				if(invoke.owner.equals(mcAppletName) && invoke.name.equals("getParameter") && invoke.desc.equals("(Ljava/lang/String;)Ljava/lang/String;")) {
					    				insn2 = insn2.getNext();
						    			if(insn2.getOpcode() == Opcodes.ALOAD) {
						    				insn2 = insn2.getNext();
							    			if(insn2 != null && insn2.getOpcode() == Opcodes.LDC && ((LdcInsnNode)insn2).cst.equals("sessionid")) {
							    				insn2 = insn2.getNext();
								    			if(insn2 != null && insn2.getOpcode() == Opcodes.INVOKEVIRTUAL) {
								    				invoke = (MethodInsnNode)insn2;
								    				if(invoke.owner.equals(mcAppletName) && invoke.name.equals("getParameter") && invoke.desc.equals("(Ljava/lang/String;)Ljava/lang/String;")) {
								    					insn2 = insn2.getNext();
								    					if(insn2 != null && insn2.getOpcode() == Opcodes.INVOKESPECIAL) {
										    				invoke = (MethodInsnNode)insn2;
										    				if(invoke.name.equals("<init>") && invoke.desc.equals("(Ljava/lang/String;Ljava/lang/String;)V")) {
										    					try {
																	session = Launch.CLASS_LOADER.loadClass(invoke.owner.replace('/', '.')).getConstructor(String.class, String.class);
																} catch (Exception e) {
																	e.printStackTrace();
																}
										    				}
										    				insn2 = insn2.getNext();
										    				if(insn2 != null && insn2.getOpcode() == Opcodes.PUTFIELD) {
										    					FieldInsnNode field = (FieldInsnNode)insn2;
										    					if(field.owner.equals(mcName)) {
										    						try {
																		sessionField = minecraft.getDeclaredField(field.name);
																	} catch (Exception e) {
																		e.printStackTrace();
																	}
										    					}
										    				}
								    					}
								    				}
								    			}
							    			}
						    			}
				    				}
				    			}
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
		//TODO Set appletMode
		//TODO Resize width and height
		Class<?>[] types = mcConstruct.getParameterTypes();
		try {
			if(types.length == 0) {
				return (Runnable)mcConstruct.newInstance();
			}
			if(types.length == 4 
				&& types[0] == Canvas.class
				&& types[1] == int.class
				&& types[2] == int.class
				&& types[3] == boolean.class) {
					return (Runnable)mcConstruct.newInstance(canvas, w, h, fullscreen);
			}
			if(types.length == 5 
				&& types[0] == Canvas.class
				&& types[1] == minecraftApplet
				&& types[2] == int.class
				&& types[3] == int.class
				&& types[4] == boolean.class) {
					return (Runnable)mcConstruct.newInstance(canvas, applet, w, h, fullscreen);
			}
			if(types.length == 6 
				&& types[0] == Component.class
				&& types[1] == Canvas.class
				&& types[2] == minecraftApplet
				&& types[3] == int.class
				&& types[4] == int.class
				&& types[5] == boolean.class) {
					return (Runnable)mcConstruct.newInstance(frame, canvas, applet, w, h, fullscreen);
			}
			if(types.length == 7 
				&& types[0] == minecraftApplet
				&& types[1] == Component.class
				&& types[2] == Canvas.class
				&& types[3] == minecraftApplet
				&& types[4] == int.class
				&& types[5] == int.class
				&& types[6] == boolean.class) {
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
	}

	public void setWidth(Object instance, int width) {
	}
	
	public void setHeight(Object instance, int height) {
	}
}
