package org.mcphackers.launchwrapper.tweak;

import static org.mcphackers.launchwrapper.util.InsnHelper.compareInsn;
import static org.mcphackers.rdi.util.InsnHelper.*;
import static org.objectweb.asm.Opcodes.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.mcphackers.launchwrapper.LaunchConfig;
import org.mcphackers.launchwrapper.LaunchConfig.LaunchParameter;
import org.mcphackers.launchwrapper.LaunchTarget;
import org.mcphackers.launchwrapper.MainLaunchTarget;
import org.mcphackers.launchwrapper.protocol.LegacyURLStreamHandler;
import org.mcphackers.launchwrapper.protocol.URLStreamHandlerProxy;
import org.mcphackers.launchwrapper.util.ClassNodeSource;
import org.mcphackers.rdi.util.IdentifyCall;
import org.mcphackers.rdi.util.NodeHelper;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;

public class VanillaTweak extends Tweak {
	public static final String MAIN_CLASS = "net/minecraft/client/main/Main";

	protected ClassNode minecraftMain;
	protected ClassNode minecraft;
	protected MethodNode run;
	private List<String> availableParameters = new ArrayList<String>();

	public VanillaTweak(ClassNodeSource source, LaunchConfig launch) {
		super(source, launch);
	}

	public boolean transform() {
		minecraftMain = source.getClass(MAIN_CLASS);
		MethodNode main = NodeHelper.getMethod(minecraftMain, "main", "([Ljava/lang/String;)V");
		if(main == null)
			return false;
		AbstractInsnNode insn = main.instructions.getLast();
		while(insn != null) {
			// Last call inside of main is minecraft.run();
			if(insn.getOpcode() == INVOKEVIRTUAL) {
				MethodInsnNode invoke = (MethodInsnNode) insn;
				minecraft = source.getClass(invoke.owner);
				run = NodeHelper.getMethod(minecraft, invoke.name, invoke.desc);
				tweakInfo("Minecraft.run()", minecraft.name + "." + invoke.name + invoke.desc);
				break;
			}
			insn = previousInsn(insn);
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
		List<LaunchParameter<?>> params = launch.getParamsAsList();
		String assets = launch.assetsDir.getString();
		for(LaunchParameter<?> param : params) {
			if(!availableParameters.contains(param.name)) {
				param.set(null);
			}
		}
		if(!availableParameters.contains("assetsDir")) {
			if(assets != null) {
				injectAssetsDirectory(assets);
			}
		}
		MethodNode init = getInit(run);
		boolean fixedTitle = replaceTitle(init);
		boolean fixedIcon = replaceIcon(init);
		for(MethodNode m : minecraft.methods) {
			fixedTitle = fixedTitle || replaceTitle(m);
			fixedIcon = fixedIcon || replaceIcon(m);
			if(fixedTitle && fixedIcon) {
				break;
			}
		}
		source.overrideClass(minecraft);
		return true;
	}

	private boolean replaceTitle(MethodNode m) {
		AbstractInsnNode insn = m.instructions.getFirst();
		while(insn != null) {
			AbstractInsnNode[] insns = fill(insn, 2);
			if(compareInsn(insns[0], LDC)
			&& compareInsn(insns[1], INVOKESTATIC, "org/lwjgl/opengl/Display", "setTitle", "(Ljava/lang/String;)V")) {
				LdcInsnNode ldc = (LdcInsnNode) insn;
				if(ldc.cst instanceof String) {
					if(launch.title.get() != null) {
						tweakInfo("Replaced title", launch.title.get());
						ldc.cst = launch.title.get();
						return true;
					}
				}
			}
			insn = nextInsn(insn);
		}
		return false;
	}

	private boolean replaceIcon(MethodNode m) {
		AbstractInsnNode insn = m.instructions.getFirst();
		while(insn != null) {
			if(launch.icon.get() != null && hasIcon(launch.icon.get())
			&& compareInsn(insn, INVOKESTATIC, "org/lwjgl/opengl/Display", "setIcon", "([Ljava/nio/ByteBuffer;)I")) {
				IdentifyCall call = new IdentifyCall((MethodInsnNode) insn);
				for(AbstractInsnNode[] arg : call.getArguments()) {
					remove(m.instructions, arg);
				}
				MethodInsnNode insert = new MethodInsnNode(INVOKESTATIC, "org/mcphackers/launchwrapper/inject/Inject", "loadIcons", "()[Ljava/nio/ByteBuffer;");
				m.instructions.insertBefore(insn, insert);
				tweakInfo("Replaced icon");
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
				if(invoke.owner.equals(minecraft.name)) {
					return NodeHelper.getMethod(minecraft, invoke.name, invoke.desc);
				}
			}
		}
		return run;
	}

	protected void injectAssetsDirectory(String assets) {
		for(MethodNode m : minecraft.methods) {
			AbstractInsnNode insn = m.instructions.getFirst();
			while(insn != null) {
				AbstractInsnNode[] insns = fill(insn, 6);
				if(compareInsn(insns[0], NEW, "java/io/File")
				&& compareInsn(insns[1], DUP)
				&& compareInsn(insns[2], ALOAD, 0)
				&& compareInsn(insns[3], GETFIELD, minecraft.name, null, "Ljava/io/File;")
				&& compareInsn(insns[4], LDC)
				&& compareInsn(insns[5], INVOKESPECIAL, "java/io/File", "<init>", "(Ljava/io/File;Ljava/lang/String;)V")) {
					LdcInsnNode ldc = (LdcInsnNode) insns[4];
					String s = (String) ldc.cst;
					if(s.startsWith("assets/")) {
						ldc.cst = s.substring(6);
						InsnList insert = new InsnList();
						insert.add(new TypeInsnNode(NEW, "java/io/File"));
						insert.add(new InsnNode(DUP));
						insert.add(new LdcInsnNode(assets));
						insert.add(new MethodInsnNode(INVOKESPECIAL, "java/io/File", "<init>", "(Ljava/lang/String;)V"));
						m.instructions.remove(insns[2]);
						m.instructions.remove(insns[3]);
						m.instructions.insert(insns[1], insert);
					} else if(s.equals("assets")) {
						ldc.cst = assets;
						m.instructions.remove(insns[2]);
						m.instructions.remove(insns[3]);
						m.instructions.set(insns[5], new MethodInsnNode(INVOKESPECIAL, "java/io/File", "<init>", "(Ljava/lang/String;)V"));
					}
					tweakInfo("Replaced assets path");
				}
				insn = nextInsn(insn);
			}
		}
	}

	public LaunchTarget getLaunchTarget() {
		URLStreamHandlerProxy.setURLStreamHandler("http", new LegacyURLStreamHandler(launch.skinProxy.get(), 11707));
		URLStreamHandlerProxy.setURLStreamHandler("https", new LegacyURLStreamHandler(launch.skinProxy.get(), 11707));
		MainLaunchTarget target = new MainLaunchTarget(MAIN_CLASS);
		target.args = launch.getArgs();
		return target;
	}

	@Override
	public ClassLoaderTweak getLoaderTweak() {
		return null;
	}

}
