package org.mcphackers.launchwrapper.inject;

import static org.mcphackers.launchwrapper.inject.InsnHelper.*;
import static org.objectweb.asm.Opcodes.*;

import org.mcphackers.launchwrapper.Launch;
import org.mcphackers.launchwrapper.loader.LaunchClassLoader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
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
	public static final String MAIN_ISOM = "net/minecraft/isom/IsomPreviewApplet";

	public ClassNode minecraft;
	public ClassNode minecraftApplet;
	public ClassNode isomApplet;
	/** Class containing username and sessionId */
	public ClassNode user;
	/** Field of user class */
	public FieldNode userField;
	/** Field that determines if Minecraft should exit */
	public FieldNode running;
	/** Frame width */
	protected FieldNode width;
	/** Frame height */
	protected FieldNode height;
	/** Working game directory */
	protected FieldNode mcDir;
	protected FieldNode mcDirIsom;
	/** public static main(String[]) */
	protected MethodNode main;
	/** Class used to instantiate a Minecraft instance */
	public ClassNode minecraftImpl;
	
	public DefaultTweak(LaunchClassLoader source, Launch launch) {
		super(source);
		this.launch = launch;
	}

	public boolean transform() {
		init();
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
		if(mcDirIsom != null) {
			for(MethodNode m : isomApplet.methods) {
				if(m.name.equals("<init>")) {
					InsnList insns = new InsnList();
					insns.add(new VarInsnNode(ALOAD, 0));
					insns.add(new TypeInsnNode(NEW, "java/io/File"));
					insns.add(new InsnNode(DUP));
					insns.add(new LdcInsnNode(launch.gameDir.getAbsolutePath()));
					insns.add(new MethodInsnNode(INVOKESPECIAL, "java/io/File", "<init>", "(Ljava/lang/String;)V"));
					insns.add(new FieldInsnNode(PUTFIELD, isomApplet.name, mcDirIsom.name, mcDirIsom.desc));
					m.instructions.insertBefore(getLastReturn(m.instructions.getLast()), insns);
				}
			}
		}
		if(width != null && height != null) {
			for(MethodNode m : minecraft.methods) {
				if(m.name.equals("<init>")) {
					InsnList insns = new InsnList();
					insns.add(new VarInsnNode(ALOAD, 0));
					insns.add(intInsn(launch.width));
					insns.add(new FieldInsnNode(PUTFIELD, minecraft.name, width.name, width.desc));
					insns.add(new VarInsnNode(ALOAD, 0));
					insns.add(intInsn(launch.height));
					insns.add(new FieldInsnNode(PUTFIELD, minecraft.name, height.name, height.desc));
					m.instructions.insertBefore(getLastReturn(m.instructions.getLast()), insns);
				}
			}
		}
		
		source.overrideClass(minecraft);
		source.overrideClass(isomApplet);
		return true;
	}

	private void init() {
		minecraftApplet = getApplet();
		minecraft = getMinecraft(minecraftApplet);
		ClassNode isomEntryPoint = source.getClass(MAIN_ISOM);
		if(isomEntryPoint != null) {
			String desc = isomEntryPoint.fields.get(0).desc;
			if(desc.startsWith("L") && desc.endsWith(";")) {
				isomApplet = source.getClass(desc.substring(1, desc.length() - 1));
			}
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
		    			insn = insn.getNext();
		    		}
		    	}
		    }
		}
		if(isomApplet != null) {
			for(FieldNode field : isomApplet.fields) {
				if(field.desc.equals("Ljava/io/File;")) {
					mcDirIsom = field;
				}
			}
		}
	}
	
	public String getLaunchTarget() {
		if(main != null) {
			return minecraft.name;
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

}
