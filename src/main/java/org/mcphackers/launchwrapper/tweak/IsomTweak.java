package org.mcphackers.launchwrapper.tweak;

import static org.mcphackers.launchwrapper.inject.InsnHelper.getLastReturn;
import static org.objectweb.asm.Opcodes.*;

import org.mcphackers.launchwrapper.Launch;
import org.mcphackers.launchwrapper.LaunchTarget;
import org.mcphackers.launchwrapper.inject.ClassNodeSource;
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

public class IsomTweak extends DefaultTweak {
	public static final String MAIN_ISOM = "net/minecraft/isom/IsomPreviewApplet";

	protected ClassNode isomApplet;
	protected FieldNode mcDirIsom;

	public IsomTweak(ClassNodeSource source, Launch launch) {
		super(source, launch);
	}
	
	protected void init() {
		super.init();
		ClassNode isomEntryPoint = source.getClass(MAIN_ISOM);
		if(isomEntryPoint != null) {
			String desc = isomEntryPoint.fields.get(0).desc;
			if(desc.startsWith("L") && desc.endsWith(";")) {
				isomApplet = source.getClass(desc.substring(1, desc.length() - 1));
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

	public boolean transform() {
		if(!super.transform()) {
			return false;
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
		source.overrideClass(isomApplet);
		return true;
	}
	
	public LaunchTarget getLaunchTarget() {
		if(isomApplet != null) {
			return new LaunchTarget(isomApplet.name, LaunchTarget.Type.APPLET);
		}
		return null;
	}

}
