package org.mcphackers.launchwrapper.tweak.injection.forge;

import static org.objectweb.asm.Opcodes.*;

import org.mcphackers.launchwrapper.LaunchConfig;
import org.mcphackers.launchwrapper.tweak.injection.Injection;
import org.mcphackers.launchwrapper.util.ClassNodeSource;
import org.mcphackers.rdi.util.NodeHelper;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodNode;

public class ForgeVersionCheck implements Injection {

    @Override
    public String name() {
        return "Forge mc version check";
    }

    @Override
    public boolean required() {
        return false;
    }

    @Override
    public boolean apply(ClassNodeSource source, LaunchConfig config) {
        ClassNode forgeClient = source.getClass("forge/MinecraftForgeClient");
        if(forgeClient == null) {
            return false;
        }
        MethodNode checkVersion = NodeHelper.getMethod(forgeClient, "checkMinecraftVersion", "(Ljava/lang/String;Ljava/lang/String;)V");
        if(checkVersion == null) {
            return false;
        }
        checkVersion.instructions.insert(new InsnNode(RETURN));
        source.overrideClass(forgeClient);
        return true;
    }

}
