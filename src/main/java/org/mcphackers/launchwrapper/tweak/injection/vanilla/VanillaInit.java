package org.mcphackers.launchwrapper.tweak.injection.vanilla;

import static org.mcphackers.launchwrapper.util.InsnHelper.*;
import static org.mcphackers.rdi.util.InsnHelper.*;
import static org.objectweb.asm.Opcodes.*;

import java.io.File;
import java.util.List;

import org.mcphackers.launchwrapper.LaunchConfig;
import org.mcphackers.launchwrapper.LaunchConfig.LaunchParameter;
import org.mcphackers.launchwrapper.tweak.VanillaTweak;
import org.mcphackers.launchwrapper.tweak.injection.InjectionWithContext;
import org.mcphackers.launchwrapper.util.ClassNodeSource;
import org.mcphackers.rdi.util.IdentifyCall;
import org.mcphackers.rdi.util.NodeHelper;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

public class VanillaInit extends InjectionWithContext<VanillaTweakContext> {

    public VanillaInit(VanillaTweakContext storage) {
        super(storage);
    }

    @Override
    public String name() {
        return "VanillaTweak init";
    }

    @Override
	public boolean required() {
		return true;
	}
    
    @Override
	public boolean apply(ClassNodeSource source, LaunchConfig config) {
		ClassNode minecraftMain = source.getClass(VanillaTweak.MAIN_CLASS);
		if(minecraftMain == null) {
			return false;
		}
		MethodNode main = NodeHelper.getMethod(minecraftMain, "main", "([Ljava/lang/String;)V");
		if(main == null) {
			return false;
		}
		MethodNode run = null;
		AbstractInsnNode insn = main.instructions.getLast();
		while(insn != null) {
			// Last call inside of main is minecraft.run();
			if(insn.getOpcode() == INVOKEVIRTUAL) {
				MethodInsnNode invoke = (MethodInsnNode) insn;
				context.minecraft = source.getClass(invoke.owner);
				run = NodeHelper.getMethod(context.minecraft, invoke.name, invoke.desc);
				break;
			}
			insn = previousInsn(insn);
		}
		for(AbstractInsnNode insn2 = main.instructions.getFirst(); insn2 != null; insn2 = nextInsn(insn2)) {
			if(compareInsn(insn2, INVOKEVIRTUAL, "joptsimple/OptionParser", "accepts", "(Ljava/lang/String;)Ljoptsimple/OptionSpecBuilder;")) {
				if(insn2.getPrevious() != null && insn2.getPrevious().getType() == AbstractInsnNode.LDC_INSN) {
					LdcInsnNode ldc = (LdcInsnNode) insn2.getPrevious();
					if(ldc.cst instanceof String) {
						context.availableParameters.add((String) ldc.cst);
					}
				}
			}
		}
		List<LaunchParameter<?>> params = config.getParamsAsList();
		for(LaunchParameter<?> param : params) {
			if(!context.availableParameters.contains(param.name)) {
				param.set(null);
			}
		}
		for(String key : config.unknownParameters.keySet()) {
			if(!context.availableParameters.contains(key)) {
				config.unknownParameters.remove(key);
			}
		}
		MethodNode init = getInit(run);
		boolean fixedTitle = replaceTitle(init, config);
		boolean fixedIcon = replaceIcon(init, config);
		for(MethodNode m : context.minecraft.methods) {
			fixedTitle = fixedTitle || replaceTitle(m, config);
			fixedIcon = fixedIcon || replaceIcon(m, config);
			if(fixedTitle && fixedIcon) {
				break;
			}
		}
		source.overrideClass(context.minecraft);
		return true;
	}

	private boolean replaceTitle(MethodNode m, LaunchConfig config) {
		AbstractInsnNode insn = m.instructions.getFirst();
		while(insn != null) {
			AbstractInsnNode[] insns = fill(insn, 2);
			if(compareInsn(insns[0], LDC)
			&& compareInsn(insns[1], INVOKESTATIC, "org/lwjgl/opengl/Display", "setTitle", "(Ljava/lang/String;)V")) {
				LdcInsnNode ldc = (LdcInsnNode) insn;
				if(ldc.cst instanceof String) {
					if(config.title.get() != null) {
						ldc.cst = config.title.get();
						return true;
					}
				}
			}
			insn = nextInsn(insn);
		}
		return false;
	}

	private boolean replaceIcon(MethodNode m, LaunchConfig config) {
		AbstractInsnNode insn = m.instructions.getFirst();
		while(insn != null) {
			if(config.icon.get() != null && hasIcon(config.icon.get())
			&& compareInsn(insn, INVOKESTATIC, "org/lwjgl/opengl/Display", "setIcon", "([Ljava/nio/ByteBuffer;)I")) {
				IdentifyCall call = new IdentifyCall((MethodInsnNode) insn);
				for(AbstractInsnNode[] arg : call.getArguments()) {
					remove(m.instructions, arg);
				}
				MethodInsnNode insert = new MethodInsnNode(INVOKESTATIC, "org/mcphackers/launchwrapper/inject/Inject", "loadIcons", "()[Ljava/nio/ByteBuffer;");
				m.instructions.insertBefore(insn, insert);
				return true;
			}
			insn = nextInsn(insn);
		}
		return false;
	}

	private boolean hasIcon(File[] icons) {
		for(File f : icons) {
			if(f.exists()) {
				return true;
			}
		}
		return false;
	}

	private MethodNode getInit(MethodNode run) {
		for(AbstractInsnNode insn : run.instructions) {
			if(insn.getType() == AbstractInsnNode.METHOD_INSN) {
				MethodInsnNode invoke = (MethodInsnNode) insn;
				if(invoke.owner.equals(context.minecraft.name)) {
					return NodeHelper.getMethod(context.minecraft, invoke.name, invoke.desc);
				}
			}
		}
		return run;
	}
    
}
