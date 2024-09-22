package org.mcphackers.launchwrapper.tweak.injection.vanilla;

import static org.mcphackers.rdi.util.InsnHelper.*;
import static org.objectweb.asm.Opcodes.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.mcphackers.launchwrapper.LaunchConfig;
import org.mcphackers.launchwrapper.tweak.VanillaTweak;
import org.mcphackers.launchwrapper.tweak.injection.Injection;
import org.mcphackers.launchwrapper.tweak.injection.MinecraftGetter;
import org.mcphackers.launchwrapper.util.ClassNodeSource;
import org.mcphackers.rdi.util.IdentifyCall;
import org.mcphackers.rdi.util.NodeHelper;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

public class VanillaTweakContext implements Injection, MinecraftGetter {
	public ClassNode minecraft;
	public MethodNode run;
	public FieldNode running;
    public List<String> availableParameters = new ArrayList<String>();
    public String[] args;

    public ClassNode getMinecraft() {
        return minecraft;
    }

    public MethodNode getRun() {
        return run;
    }

    public FieldNode getIsRunning() {
        return running;
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
		AbstractInsnNode insn = main.instructions.getLast();
		while(insn != null) {
			// Last call inside of main is minecraft.run();
			if(insn.getOpcode() == INVOKEVIRTUAL) {
				MethodInsnNode invoke = (MethodInsnNode) insn;
				minecraft = source.getClass(invoke.owner);
				run = NodeHelper.getMethod(minecraft, invoke.name, invoke.desc);
				break;
			}
			insn = previousInsn(insn);
		}
		if(run == null) {
			return false;
		}
		insn = run.instructions.getFirst();
		while(insn != null) {
			if(insn.getOpcode() == Opcodes.PUTFIELD) {
				FieldInsnNode putField = (FieldInsnNode) insn;
				if("Z".equals(putField.desc)) {
					running = NodeHelper.getField(minecraft, putField.name, putField.desc);
				}
				break;
			}
			insn = nextInsn(insn);
		}
		for(AbstractInsnNode insn2 = main.instructions.getFirst(); insn2 != null; insn2 = nextInsn(insn2)) {
			if(compareInsn(insn2, INVOKEVIRTUAL, "joptsimple/OptionParser", "accepts", "(Ljava/lang/String;)Ljoptsimple/OptionSpecBuilder;")) {
				if(insn2.getPrevious() != null && insn2.getPrevious().getType() == AbstractInsnNode.LDC_INSN) {
					LdcInsnNode ldc = (LdcInsnNode) insn2.getPrevious();
					if(ldc.cst instanceof String) {
						availableParameters.add((String) ldc.cst);
					}
				}
			}
		}
		String[] params = config.getArgs();
		List<String> newArgs = new ArrayList<String>();
		for(int i = 0; i < params.length; i++) {
			if(!params[i].startsWith("--")) {
				continue;
			}
			if(!availableParameters.contains(params[i].substring(2))) {
				continue;
			}
			newArgs.add(params[i]);
			if(i+1 < params.length && !params[i+1].startsWith("--")) {
				newArgs.add(params[i+1]);
				i++;
			}
		}
		String[] arr = new String[newArgs.size()];
		args = newArgs.toArray(arr);

		MethodNode init = getInit(run);
		boolean fixedTitle = replaceTitle(init, config);
		boolean fixedIcon = replaceIcon(init, config);
		for(MethodNode m : minecraft.methods) {
			fixedTitle = fixedTitle || replaceTitle(m, config);
			fixedIcon = fixedIcon || replaceIcon(m, config);
			if(fixedTitle && fixedIcon) {
				break;
			}
		}
		source.overrideClass(minecraft);
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
				MethodInsnNode insert = new MethodInsnNode(INVOKESTATIC, "org/mcphackers/launchwrapper/util/IconUtils", "loadIcon", "()[Ljava/nio/ByteBuffer;");
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
		for(AbstractInsnNode insn = run.instructions.getFirst(); insn != null; insn = nextInsn(insn)) {
			if(insn.getType() == AbstractInsnNode.METHOD_INSN) {
				MethodInsnNode invoke = (MethodInsnNode) insn;
				if(invoke.owner.equals(minecraft.name)) {
					return NodeHelper.getMethod(minecraft, invoke.name, invoke.desc);
				}
			}
		}
		return run;
	}
    
}
