package org.mcphackers.launchwrapper.tweak;

import static org.mcphackers.launchwrapper.inject.InsnHelper.*;
import static org.objectweb.asm.Opcodes.*;

import org.mcphackers.launchwrapper.AppletLaunchTarget;
import org.mcphackers.launchwrapper.LaunchConfig;
import org.mcphackers.launchwrapper.LaunchTarget;
import org.mcphackers.launchwrapper.inject.ClassNodeSource;
import org.mcphackers.launchwrapper.inject.Inject;
import org.mcphackers.launchwrapper.loader.LaunchClassLoader;
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

public class IsomTweak extends LegacyTweak {
	public static final String MAIN_ISOM = "net/minecraft/isom/IsomPreviewApplet";

	protected ClassNode isomApplet;
	protected ClassNode isomCanvas;
	protected FieldNode mcDirIsom;

	public IsomTweak(ClassNodeSource source, LaunchConfig launch) {
		super(source, launch);
	}

	protected void init() {
		super.init();
		isomApplet = source.getClass(MAIN_ISOM);
		if(isomApplet != null) {
			String desc = isomApplet.fields.get(0).desc;
			if(desc.startsWith("L") && desc.endsWith(";")) {
				isomCanvas = source.getClass(desc.substring(1, desc.length() - 1));
			}
		}
		if(isomCanvas != null) {
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
			for(MethodNode m : isomCanvas.methods) {
				if(m.name.equals("<init>")) {
					InsnList insns = new InsnList();
					insns.add(new VarInsnNode(ALOAD, 0));
					insns.add(new TypeInsnNode(NEW, "java/io/File"));
					insns.add(new InsnNode(DUP));
					insns.add(new LdcInsnNode(launch.gameDir.getString()));
					insns.add(new MethodInsnNode(INVOKESPECIAL, "java/io/File", "<init>", "(Ljava/lang/String;)V"));
					insns.add(new FieldInsnNode(PUTFIELD, isomCanvas.name, mcDirIsom.name, mcDirIsom.desc));
					m.instructions.insertBefore(getLastReturn(m.instructions.getLast()), insns);
				}
			}
		}
		source.overrideClass(isomCanvas);
		return true;
	}

	public LaunchTarget getLaunchTarget(LaunchClassLoader loader) {
		if(isomApplet != null) {
			AppletLaunchTarget launchTarget = new AppletLaunchTarget(loader, isomApplet.name);
			if(launch.title.get() != null) {
				launchTarget.setTitle(launch.title.get());
			} else {
				launchTarget.setTitle("IsomPreview");
			}
			launchTarget.setIcon(Inject.getIcon());
			launchTarget.setResolution(launch.width.get(), launch.height.get());
			return launchTarget;
		}
		return null;
	}

}
