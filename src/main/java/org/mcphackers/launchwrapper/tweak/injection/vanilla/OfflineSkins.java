package org.mcphackers.launchwrapper.tweak.injection.vanilla;

import static org.mcphackers.launchwrapper.util.asm.InsnHelper.*;
import static org.objectweb.asm.Opcodes.*;

import org.mcphackers.launchwrapper.LaunchConfig;
import org.mcphackers.launchwrapper.tweak.injection.InjectionWithContext;
import org.mcphackers.launchwrapper.tweak.injection.MinecraftGetter;
import org.mcphackers.launchwrapper.util.ClassNodeSource;
import org.mcphackers.launchwrapper.util.asm.NodeHelper;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

public class OfflineSkins extends InjectionWithContext<MinecraftGetter> {
	public OfflineSkins(MinecraftGetter storage) {
		super(storage);
	}

	public String name() {
		return "Offline skins";
	}

	public boolean required() {
		return false;
	}

	public boolean apply(ClassNodeSource source, LaunchConfig config) {
		if (config.disableSkinFix.get()) {
			return false;
		}
		MethodNode init = context.getInit();
		if (init == null) {
			return false;
		}
		ClassNode mc = context.getMinecraft();
		for (MethodNode m : NodeHelper.getConstructors(mc)) {
			AbstractInsnNode insn = getFirst(m.instructions);
			for (; insn != null; insn = nextInsn(insn)) {
				if (insn.getOpcode() != INVOKEVIRTUAL && insn.getOpcode() != INVOKESPECIAL) {
					continue;
				}
				MethodInsnNode mInsn = (MethodInsnNode)insn;
				// authlib isn't obfuscated. We can rely on hardcoded names
				if (compareInsn(mInsn, -1, "com/mojang/authlib/yggdrasil/YggdrasilAuthenticationService", "createMinecraftSessionService", "()Lcom/mojang/authlib/minecraft/MinecraftSessionService;")) {
					m.instructions.set(insn, new MethodInsnNode(INVOKESTATIC, "org/mcphackers/launchwrapper/inject/SessionService", "redirectSessionService", "(Lcom/mojang/authlib/yggdrasil/YggdrasilAuthenticationService;)Lcom/mojang/authlib/minecraft/MinecraftSessionService;"));
					source.overrideClass(mc);
					return true;
				}
			}
		}
		return false;
	}
}
