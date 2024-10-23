package org.mcphackers.launchwrapper.tweak.injection.vanilla;

import static org.objectweb.asm.Opcodes.*;

import org.mcphackers.launchwrapper.LaunchConfig;
import org.mcphackers.launchwrapper.tweak.injection.Injection;
import org.mcphackers.launchwrapper.util.ClassNodeSource;
import org.mcphackers.launchwrapper.util.asm.NodeHelper;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodNode;

public class ChangeBrand implements Injection {
	public static final String BRAND_RETRIEVER = "net/minecraft/client/ClientBrandRetriever";
	public static final String BRAND_RETRIEVER_SERVER = "net/minecraft/server/MinecraftServer";

	public String name() {
		return "Changed brand";
	}

	public boolean required() {
		return false;
	}

	public boolean apply(ClassNodeSource source, LaunchConfig config) {
		boolean b = false;
		ClassNode clientBrand = source.getClass(BRAND_RETRIEVER);
		if (clientBrand != null) {
			MethodNode getClientModName = NodeHelper.getMethod(clientBrand, "getClientModName", "()Ljava/lang/String;");
			if (getClientModName != null) {
				InsnList insns = new InsnList();
				insns.add(new LdcInsnNode(config.brand.get() != null ? config.brand.get() : "launchwrapper"));
				insns.add(new InsnNode(ARETURN));
				getClientModName.instructions = insns;
				source.overrideClass(clientBrand);
				b = true;
			}
		}
		ClassNode serverBrand = source.getClass(BRAND_RETRIEVER_SERVER);
		if (serverBrand != null) {
			MethodNode getServerModName = NodeHelper.getMethod(serverBrand, "getServerModName", "()Ljava/lang/String;");
			if (getServerModName != null) {
				InsnList insns = new InsnList();
				insns.add(new LdcInsnNode(config.brand.get() != null ? config.brand.get() : "launchwrapper"));
				insns.add(new InsnNode(ARETURN));
				getServerModName.instructions = insns;
				source.overrideClass(serverBrand);
				b = true;
			}
		}
		return b;
	}
}
