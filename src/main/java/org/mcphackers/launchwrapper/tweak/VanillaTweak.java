package org.mcphackers.launchwrapper.tweak;

import static org.mcphackers.launchwrapper.inject.InsnHelper.compareInsn;
import static org.mcphackers.launchwrapper.inject.InsnHelper.fill;
import static org.mcphackers.launchwrapper.inject.InsnHelper.nextInsn;
import static org.mcphackers.launchwrapper.inject.InsnHelper.previousInsn;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.DUP;
import static org.objectweb.asm.Opcodes.GETFIELD;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Opcodes.LDC;
import static org.objectweb.asm.Opcodes.NEW;

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
	
	protected LaunchConfig launch;
	protected ClassNode minecraftMain;
	protected ClassNode minecraft;
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
				minecraft = source.getClass(((MethodInsnNode)insn).owner);
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
		source.overrideClass(minecraft);
		return true;
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
