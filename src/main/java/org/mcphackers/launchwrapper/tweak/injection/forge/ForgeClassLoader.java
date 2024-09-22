package org.mcphackers.launchwrapper.tweak.injection.forge;

import static org.objectweb.asm.Opcodes.*;

import org.mcphackers.launchwrapper.LaunchConfig;
import org.mcphackers.launchwrapper.tweak.injection.Injection;
import org.mcphackers.launchwrapper.util.ClassNodeSource;
import org.mcphackers.rdi.util.NodeHelper;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

public class ForgeClassLoader implements Injection {

	@Override
	public String name() {
        return "Forge ClassLoader patch";
	}

	@Override
	public boolean required() {
		return false;
	}

	@Override
	public boolean apply(ClassNodeSource source, LaunchConfig config) {
        ClassNode modClassLoader = source.getClass("net/minecraftforge/fml/common/ModClassLoader");
        if(modClassLoader == null) {
            return false;
        }
        MethodNode init = NodeHelper.getMethod(modClassLoader, "<init>", "(Ljava/lang/ClassLoader;)V");
        if(init == null) {
            return false;
        }
        InsnList inject = new InsnList();
        inject.add(new FieldInsnNode(GETSTATIC, "net/minecraft/launchwrapper/Launch", "classLoader", "Lnet/minecraft/launchwrapper/LaunchClassLoader;"));
        inject.add(new VarInsnNode(ASTORE, 1));
        init.instructions.insert(inject);
        source.overrideClass(modClassLoader);
        return true;
	}

}
