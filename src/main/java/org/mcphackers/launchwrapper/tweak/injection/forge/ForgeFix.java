package org.mcphackers.launchwrapper.tweak.injection.forge;

import static org.mcphackers.launchwrapper.util.asm.InsnHelper.*;
import static org.objectweb.asm.Opcodes.*;

import org.mcphackers.launchwrapper.LaunchConfig;
import org.mcphackers.launchwrapper.tweak.injection.Injection;
import org.mcphackers.launchwrapper.util.ClassNodeSource;
import org.mcphackers.launchwrapper.util.asm.NodeHelper;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

public class ForgeFix implements Injection {

	public String name() {
		return "Forge patch";
	}

	public boolean required() {
		return false;
	}

	public boolean apply(ClassNodeSource source, LaunchConfig config) {
		ClassNode eventSubscriptionTransformer = source.getClass("net/minecraftforge/fml/common/asm/transformers/EventSubscriptionTransformer");
		if (eventSubscriptionTransformer == null) {
			return false;
		}
		MethodNode buildEvents = NodeHelper.getMethod(eventSubscriptionTransformer, "buildEvents", "(Lorg/objectweb/asm/tree/ClassNode;)Z");
		if (buildEvents == null) {
			return false;
		}
		for (AbstractInsnNode insn = getFirst(buildEvents.instructions); insn != null; insn = nextInsn(insn)) {
			if (compareInsn(insn, GETFIELD, "org/objectweb/asm/tree/ClassNode", "superName", "Ljava/lang/String;") &&
				compareInsn(nextInsn(insn), INVOKESTATIC, "org/objectweb/asm/Type", "getType", "(Ljava/lang/String;)Lorg/objectweb/asm/Type;")) {
				// Forge incorrectly uses getType here. getObjectType makes it compatible with asm 9.7+
				buildEvents.instructions.set(nextInsn(insn), new MethodInsnNode(INVOKESTATIC, "org/objectweb/asm/Type", "getObjectType", "(Ljava/lang/String;)Lorg/objectweb/asm/Type;"));
				break;
			}
		}
		source.overrideClass(eventSubscriptionTransformer);
		return true;
	}
}
