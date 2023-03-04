package org.mcphackers.launchwrapper.tweak;

import static org.mcphackers.launchwrapper.inject.InsnHelper.*;
import static org.objectweb.asm.Opcodes.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.mcphackers.launchwrapper.LaunchConfig;
import org.mcphackers.launchwrapper.LaunchConfig.LaunchParameter;
import org.mcphackers.launchwrapper.LaunchTarget;
import org.mcphackers.launchwrapper.MainLaunchTarget;
import org.mcphackers.launchwrapper.inject.ClassNodeSource;
import org.mcphackers.launchwrapper.inject.InjectUtils;
import org.mcphackers.launchwrapper.loader.LaunchClassLoader;
import org.mcphackers.launchwrapper.protocol.LegacyURLStreamHandler;
import org.mcphackers.launchwrapper.protocol.LegacyURLStreamHandler.SkinType;
import org.mcphackers.launchwrapper.protocol.URLStreamHandlerProxy;
import org.mcphackers.rdi.util.IdentifyCall;
import org.objectweb.asm.tree.*;

public class VanillaTweak extends Tweak {
	public static final String MAIN_CLASS = "net/minecraft/client/main/Main";
	
	protected LaunchConfig launch;
	protected ClassNode minecraftMain;
	protected ClassNode minecraft;
	protected MethodNode run;
	private List<String> availableParameters = new ArrayList<String>();

	public VanillaTweak(ClassNodeSource source, LaunchConfig launch) {
		super(source);
		this.launch = launch;
	}

	public boolean transform() {
		minecraftMain = source.getClass(MAIN_CLASS);
		MethodNode main = InjectUtils.getMethod(minecraftMain, "main", "([Ljava/lang/String;)V");
		if(main == null) return false;
		AbstractInsnNode insn = main.instructions.getLast();
		while(insn != null) {
			// Last call inside of main is minecraft.run();
			if(insn.getOpcode() == INVOKEVIRTUAL) {
				MethodInsnNode invoke = (MethodInsnNode)insn; 
				minecraft = source.getClass(invoke.owner);
				run = InjectUtils.getMethod(minecraft, invoke.name, invoke.desc);
				break;
			}
			insn = previousInsn(insn);
		}
		for(AbstractInsnNode insn2 = main.instructions.getFirst(); insn2 != null; insn2 = nextInsn(insn2)) {
			if(compareInsn(insn2, INVOKEVIRTUAL, "joptsimple/OptionParser", "accepts", "(Ljava/lang/String;)Ljoptsimple/OptionSpecBuilder;")) {
				if(insn2.getPrevious() != null && insn2.getPrevious().getType() == AbstractInsnNode.LDC_INSN) {
					LdcInsnNode ldc = (LdcInsnNode)insn2.getPrevious();
					if(ldc.cst instanceof String) {
						availableParameters.add((String)ldc.cst);
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
		replaceIconAndTitle(getInit(run));
		source.overrideClass(minecraft);
		return true;
	}
	
	private void replaceIconAndTitle(MethodNode init) {
		AbstractInsnNode insn = init.instructions.getFirst();
		while(insn != null) {
			AbstractInsnNode[] insns = fill(insn, 6);
    		if(compareInsn(insns[0], LDC)
    		&& compareInsn(insns[1], INVOKESTATIC, "org/lwjgl/opengl/Display", "setTitle", "(Ljava/lang/String;)V")) {
    			LdcInsnNode ldc = (LdcInsnNode)insn;
    			if(ldc.cst instanceof String) {
    				if(launch.title.get() != null) {
    					debugInfo("Replaced title");
    					ldc.cst = launch.title.get();
    				}
    			}
    		}
    		if(launch.icon.get() != null && hasIcon(launch.icon.get())
    		&& compareInsn(insn, INVOKESTATIC, "org/lwjgl/opengl/Display", "setIcon", "([Ljava/nio/ByteBuffer;)I")) {
    			IdentifyCall call = new IdentifyCall((MethodInsnNode)insn);
    			for(AbstractInsnNode[] arg : call.getArguments()) {
        			remove(init.instructions, arg);
    			}
    			MethodInsnNode insert = new MethodInsnNode(INVOKESTATIC, "org/mcphackers/launchwrapper/inject/Inject", "loadIcons", "()[Ljava/nio/ByteBuffer;");
    			init.instructions.insertBefore(insn, insert);
    			debugInfo("Replaced icon");
    		}
			insn = nextInsn(insn);
		}
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
				MethodInsnNode invoke = (MethodInsnNode)insn;
				if(invoke.owner.equals(minecraft.name)) {
					return InjectUtils.getMethod(minecraft, invoke.name, invoke.desc);
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
					LdcInsnNode ldc = (LdcInsnNode)insns[4];
					String s = (String)ldc.cst;
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
					} else if (s.equals("assets")) {
						ldc.cst = assets;
						m.instructions.remove(insns[2]);
						m.instructions.remove(insns[3]);
						m.instructions.set(insns[5], new MethodInsnNode(INVOKESPECIAL, "java/io/File", "<init>", "(Ljava/lang/String;)V"));
					}
				}
				insn = nextInsn(insn);
			}
		}
	}

	public LaunchTarget getLaunchTarget(LaunchClassLoader loader) {
		URLStreamHandlerProxy.setURLStreamHandler("http", new LegacyURLStreamHandler(SkinType.get(launch.skinProxy.get()), 11707));
		MainLaunchTarget target = new MainLaunchTarget(loader, MAIN_CLASS);
		target.args = launch.getArgs();
		return target;
	}

}
