package org.mcphackers.launchwrapper.tweak.injection.vanilla;

import static org.objectweb.asm.Opcodes.*;

import org.mcphackers.launchwrapper.LaunchConfig;
import org.mcphackers.launchwrapper.tweak.injection.Injection;
import org.mcphackers.launchwrapper.util.ClassNodeSource;
import org.mcphackers.rdi.util.NodeHelper;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodNode;

public class ChangeBrand implements Injection {
	public static final String BRAND_RETRIEVER = "net/minecraft/client/ClientBrandRetriever";

    @Override
	public String name() {
		return "Changed brand";
	}

    @Override
	public boolean required() {
		return false;
	}

    @Override
    public boolean apply(ClassNodeSource source, LaunchConfig config) {
		ClassNode clientBrand = source.getClass(BRAND_RETRIEVER);
		if(clientBrand != null) {
			MethodNode getClientModName = NodeHelper.getMethod(clientBrand, "getClientModName", "()Ljava/lang/String;");
			if(getClientModName != null) {
				InsnList insns = new InsnList();
				insns.add(new LdcInsnNode(config.brand.get() != null ? config.brand.get() : "launchwrapper"));
				insns.add(new InsnNode(ARETURN));
				getClientModName.instructions = insns;
				source.overrideClass(clientBrand);
                return true;
			}
		}
        return false;
	}
}
