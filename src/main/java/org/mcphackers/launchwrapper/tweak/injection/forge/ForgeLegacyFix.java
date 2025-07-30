package org.mcphackers.launchwrapper.tweak.injection.forge;

import static org.mcphackers.launchwrapper.util.asm.InsnHelper.*;
import static org.mcphackers.launchwrapper.util.asm.NodeHelper.*;
import static org.objectweb.asm.Opcodes.*;

import org.mcphackers.launchwrapper.LaunchConfig;
import org.mcphackers.launchwrapper.tweak.injection.Injection;
import org.mcphackers.launchwrapper.util.ClassNodeSource;
import org.mcphackers.launchwrapper.util.asm.NodeHelper;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

public class ForgeLegacyFix implements Injection {

	public String name() {
		return "Forge legacy patch";
	}

	public boolean required() {
		return false;
	}

	public boolean apply(ClassNodeSource source, LaunchConfig config) {
		ClassNode applet = source.getClass("net/minecraft/client/MinecraftApplet");
		ClassNode relaunchCL = source.getClass("cpw/mods/fml/relauncher/RelaunchClassLoader");
		if (applet == null || relaunchCL == null) {
			return false;
		}
		MethodNode appletInit = NodeHelper.getMethod(applet, "init", "()V");
		if (appletInit == null) {
			return false;
		}
		MethodNode getClassBytes = NodeHelper.getMethod(relaunchCL, "getClassBytes", "(Ljava/lang/String;)[B");
		if (getClassBytes == null) {
			return false;
		}
		for (AbstractInsnNode insn = getFirst(getClassBytes.instructions); insn != null; insn = nextInsn(insn)) {
			if (compareInsn(insn, INVOKEVIRTUAL, null, "findResource", "(Ljava/lang/String;)Ljava/net/URL;")) {
				getClassBytes.instructions.set(insn, new MethodInsnNode(INVOKESPECIAL, "cpw/mods/fml/relauncher/RelaunchClassLoader", "findResource", "(Ljava/lang/String;)Ljava/net/URL;"));
			}
		}
		MethodNode init = NodeHelper.getMethod(relaunchCL, "<init>", "([Ljava/net/URL;)V");
		for (AbstractInsnNode insn = getFirst(init.instructions); insn != null; insn = nextInsn(insn)) {
			if (compareInsn(insn, INVOKESPECIAL, null, "addClassLoaderExclusion", "(Ljava/lang/String;)V")) {
				InsnList insert = new InsnList();
				insert.add(new VarInsnNode(ALOAD, 0));
				insert.add(new LdcInsnNode("org.mcphackers.launchwrapper."));
				insert.add(new MethodInsnNode(INVOKESPECIAL, "cpw/mods/fml/relauncher/RelaunchClassLoader", "addClassLoaderExclusion", "(Ljava/lang/String;)V"));
				init.instructions.insert(insn, insert);
				break;
			}
		}
		MethodNode findRes = newMethod(relaunchCL, ACC_PUBLIC, "findResource", "(Ljava/lang/String;)Ljava/net/URL;", null, null, false);
		findRes.instructions.add(new VarInsnNode(ALOAD, 0));
		findRes.instructions.add(new FieldInsnNode(GETFIELD, "cpw/mods/fml/relauncher/RelaunchClassLoader", "parent", "Ljava/lang/ClassLoader;"));
		findRes.instructions.add(new VarInsnNode(ALOAD, 1));
		findRes.instructions.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/ClassLoader", "getResource", "(Ljava/lang/String;)Ljava/net/URL;"));
		findRes.instructions.add(new InsnNode(ARETURN));
		appletInit.instructions = new InsnList();
		appletInit.instructions.add(new VarInsnNode(ALOAD, 0));
		appletInit.instructions.add(new MethodInsnNode(INVOKESPECIAL, "net/minecraft/client/MinecraftApplet", "fmlInitReentry", "()V"));
		appletInit.instructions.add(new InsnNode(RETURN));
		source.overrideClass(applet);
		source.overrideClass(relaunchCL);
		return true;
	}
}
