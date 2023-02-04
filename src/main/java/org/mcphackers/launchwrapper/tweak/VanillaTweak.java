package org.mcphackers.launchwrapper.tweak;

import org.mcphackers.launchwrapper.LaunchConfig;
import org.mcphackers.launchwrapper.LaunchTarget;
import org.mcphackers.launchwrapper.MainLaunchTarget;
import org.mcphackers.launchwrapper.inject.ClassNodeSource;
import org.mcphackers.launchwrapper.loader.LaunchClassLoader;
import org.mcphackers.launchwrapper.protocol.LegacyURLStreamHandler;
import org.mcphackers.launchwrapper.protocol.LegacyURLStreamHandler.SkinType;
import org.mcphackers.launchwrapper.protocol.URLStreamHandlerProxy;

public class VanillaTweak extends Tweak {
	public static final String MAIN_CLASS = "net/minecraft/client/main/Main";
	
	protected LaunchConfig launch;
//	protected ClassNode minecraftMain;
//	protected ClassNode minecraft;

	public VanillaTweak(ClassNodeSource source, LaunchConfig launch) {
		super(source);
		this.launch = launch;
	}

	public boolean transform() {
//		minecraftMain = source.getClass(MAIN_CLASS);
//		MethodNode main = InjectUtils.getMethod(minecraftMain, "main", "([Ljava/lang/String;)V");
//		if(main == null) return false;
//		AbstractInsnNode insn = main.instructions.getLast();
//		while(insn != null) {
//			// Last call inside of main is minecraft.run();
//			if(compareInsn(insn, INVOKEVIRTUAL)) {
//				minecraft = source.getClass(((MethodInsnNode)insn).owner);
//				break;
//			}
//			insn = insn.getPrevious();
//		}
		return true;
	}

	public LaunchTarget getLaunchTarget(LaunchClassLoader loader) {
		URLStreamHandlerProxy.setURLStreamHandler("http", new LegacyURLStreamHandler(SkinType.get(launch.skinProxy.get()), 11707));
		MainLaunchTarget target = new MainLaunchTarget(loader, MAIN_CLASS);
		target.args = launch.getArgs();
		return target;
	}

}
