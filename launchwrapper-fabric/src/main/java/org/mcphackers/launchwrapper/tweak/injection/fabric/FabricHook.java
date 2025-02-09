package org.mcphackers.launchwrapper.tweak.injection.fabric;

import static org.mcphackers.launchwrapper.util.asm.InsnHelper.*;
import static org.objectweb.asm.Opcodes.*;

import net.fabricmc.loader.impl.game.minecraft.Hooks;

import org.mcphackers.launchwrapper.LaunchConfig;
import org.mcphackers.launchwrapper.tweak.injection.Injection;
import org.mcphackers.launchwrapper.tweak.injection.MinecraftGetter;
import org.mcphackers.launchwrapper.util.ClassNodeSource;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

public class FabricHook implements Injection {

	private MinecraftGetter minecraftGetter;

	public FabricHook(MinecraftGetter minecraftGetter) {
		this.minecraftGetter = minecraftGetter;
	}

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

		ClassNode minecraft = minecraftGetter.getMinecraft();
		if (minecraft == null) {
			return false;
		}
		boolean success = false;
		for (MethodNode m : minecraft.methods) {
			if (m.name.equals("<init>")) {
				AbstractInsnNode injectPoint = getLastReturn(getLast(m.instructions));
				for(AbstractInsnNode insn = getFirst(m.instructions); insn != null; insn = nextInsn(insn)) {
					if(compareInsn(insn, INVOKESTATIC, "java/lang/Thread", "currentThread", "()Ljava/lang/Thread;")) {
						injectPoint = insn;
					}
				}
				InsnList insns = new InsnList();
				insns.add(getGameDirectory(config));
				insns.add(new VarInsnNode(ALOAD, 0));
				insns.add(new MethodInsnNode(Opcodes.INVOKESTATIC, Hooks.INTERNAL_NAME, "startClient",
											 "(Ljava/io/File;Ljava/lang/Object;)V", false));
				m.instructions.insertBefore(injectPoint, insns);
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
}
