package org.mcphackers.launchwrapper.tweak;

import static org.mcphackers.launchwrapper.util.InsnHelper.*;
import static org.objectweb.asm.Opcodes.*;

import org.mcphackers.launchwrapper.LaunchConfig;
import org.mcphackers.launchwrapper.LaunchTarget;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;

import net.fabricmc.loader.impl.game.minecraft.Hooks;

public class FabricLoaderTweak extends Tweak {

    protected Tweak baseTweak;

    public FabricLoaderTweak(Tweak baseTweak, LaunchConfig launch) {
        super(baseTweak.source, launch);
        this.baseTweak = baseTweak;
    }

	private InsnList getGameDirectory() {
		InsnList insns = new InsnList();
		insns.add(new TypeInsnNode(NEW, "java/io/File"));
		insns.add(new InsnNode(DUP));
		insns.add(new LdcInsnNode(launch.gameDir.getString()));
		insns.add(new MethodInsnNode(INVOKESPECIAL, "java/io/File", "<init>", "(Ljava/lang/String;)V"));
		return insns;
	}

    @Override
    public boolean transform() {
        if(!baseTweak.transform()) {
            return false;
        }
        if(baseTweak instanceof LegacyTweak) {
            LegacyTweak tweak = (LegacyTweak)baseTweak;

            for(MethodNode m : tweak.minecraft.methods) {
                if(m.name.equals("<init>")) {
                    InsnList insns = new InsnList();
                    insns.add(getGameDirectory());
                    insns.add(new IntInsnNode(ALOAD, 0));
                    insns.add(new MethodInsnNode(Opcodes.INVOKESTATIC, Hooks.INTERNAL_NAME, "startClient",
                        "(Ljava/io/File;Ljava/lang/Object;)V", false));
                        m.instructions.insertBefore(getLastReturn(m.instructions.getLast()), insns);
                    source.overrideClass(tweak.minecraft);
                    tweakInfo("Adding fabric hooks");
                }
            }
        }
        return true;
    }

    @Override
    public ClassLoaderTweak getLoaderTweak() {
        return baseTweak.getLoaderTweak();
    }

    @Override
    public LaunchTarget getLaunchTarget() {
        return baseTweak.getLaunchTarget();
    }
    
}
