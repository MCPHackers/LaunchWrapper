package org.mcphackers.launchwrapper.tweak.injection.fabric;

import static org.mcphackers.launchwrapper.util.InsnHelper.*;
import static org.objectweb.asm.Opcodes.*;

import org.mcphackers.launchwrapper.LaunchConfig;
import org.mcphackers.launchwrapper.tweak.LegacyTweak;
import org.mcphackers.launchwrapper.tweak.VanillaTweak;
import org.mcphackers.launchwrapper.tweak.injection.Injection;
import org.mcphackers.launchwrapper.util.ClassNodeSource;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;

import net.fabricmc.loader.impl.game.minecraft.Hooks;

public class FabricHook implements Injection {

    @Override
    public String name() {
        return "Fabric hook";
    }

    @Override
    public boolean required() {
        return true;
    }

    @Override
    public boolean apply(ClassNodeSource source, LaunchConfig config) {
        
		ClassNode minecraftApplet = getApplet(source);
		ClassNode minecraft = getMinecraft(source, minecraftApplet);
		if(minecraft == null) {
			return false;
		}
        boolean success = false;
        for(MethodNode m : minecraft.methods) {
            if(m.name.equals("<init>")) {
                InsnList insns = new InsnList();
                insns.add(getGameDirectory(config));
                insns.add(new IntInsnNode(ALOAD, 0));
                insns.add(new MethodInsnNode(Opcodes.INVOKESTATIC, Hooks.INTERNAL_NAME, "startClient",
                    "(Ljava/io/File;Ljava/lang/Object;)V", false));
                    m.instructions.insertBefore(getLastReturn(m.instructions.getLast()), insns);
                source.overrideClass(minecraft);
                success = true;
            }
        }
        return success;
    }

	private InsnList getGameDirectory(LaunchConfig config) {
		InsnList insns = new InsnList();
		insns.add(new TypeInsnNode(NEW, "java/io/File"));
		insns.add(new InsnNode(DUP));
		insns.add(new LdcInsnNode(config.gameDir.getString()));
		insns.add(new MethodInsnNode(INVOKESPECIAL, "java/io/File", "<init>", "(Ljava/lang/String;)V"));
		return insns;
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
        ClassNode cls = source.getClass(VanillaTweak.MAIN_CLASS);
        if(cls != null && cls.interfaces.contains("java/lang/Runnable")) {
            launchTarget = cls;
        }
        if(launchTarget == null) {
            for(String main : LegacyTweak.MAIN_CLASSES) {
                cls = source.getClass(main);
                if(cls != null && cls.interfaces.contains("java/lang/Runnable")) {
                    launchTarget = cls;
                    break;
                }
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
