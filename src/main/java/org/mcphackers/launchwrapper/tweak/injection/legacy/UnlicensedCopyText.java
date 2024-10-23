package org.mcphackers.launchwrapper.tweak.injection.legacy;

import static org.mcphackers.launchwrapper.util.asm.InsnHelper.*;
import static org.objectweb.asm.Opcodes.*;

import org.mcphackers.launchwrapper.LaunchConfig;
import org.mcphackers.launchwrapper.tweak.injection.InjectionWithContext;
import org.mcphackers.launchwrapper.tweak.injection.MinecraftGetter;
import org.mcphackers.launchwrapper.util.ClassNodeSource;
import org.mcphackers.launchwrapper.util.asm.NodeHelper;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Restores "unlicensed copy" text within the version HUD
 */
public class UnlicensedCopyText extends InjectionWithContext<MinecraftGetter> {

	public UnlicensedCopyText(MinecraftGetter context) {
		super(context);
	}

	public String name() {
		return "Unlicensed Copy text";
	}

	public boolean required() {
		return false;
	}

	public boolean apply(ClassNodeSource source, LaunchConfig config) {
		if (!config.unlicensedCopy.get()) {
			return false;
		}
		ClassNode minecraft = context.getMinecraft();
		MethodNode clinit = NodeHelper.getMethod(minecraft, "<clinit>", "()V");
		if (clinit == null) {
			return false;
		}
		// TODO make better field detection
		for (AbstractInsnNode insn = getFirst(clinit.instructions); insn != null; insn = nextInsn(insn)) {
			if (compareInsn(insn, LCONST_0) &&
				compareInsn(nextInsn(insn), PUTSTATIC, minecraft.name, null, "J")) {
				clinit.instructions.set(insn, longInsn(1));
				source.overrideClass(minecraft);
				return true;
			}
		}

		return false;
	}
}
