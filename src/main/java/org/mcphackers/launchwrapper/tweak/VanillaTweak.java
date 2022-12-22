package org.mcphackers.launchwrapper.tweak;

import static org.mcphackers.launchwrapper.inject.InsnHelper.compareInsn;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;

import org.mcphackers.launchwrapper.LaunchConfig;
import org.mcphackers.launchwrapper.LaunchTarget;
import org.mcphackers.launchwrapper.MainLaunchTarget;
import org.mcphackers.launchwrapper.inject.ClassNodeSource;
import org.mcphackers.launchwrapper.inject.InjectUtils;
import org.mcphackers.launchwrapper.loader.LaunchClassLoader;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

public class VanillaTweak extends Tweak {
	public static final String MAIN_CLASS = "net/minecraft/client/main/Main";
	
	protected LaunchConfig launch;
	protected ClassNode minecraftMain;
	protected ClassNode minecraft;

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
			if(compareInsn(insn, INVOKEVIRTUAL)) {
				minecraft = source.getClass(((MethodInsnNode)insn).owner);
				break;
			}
			insn = insn.getPrevious();
		}
		return true;
	}

	public LaunchTarget getLaunchTarget(LaunchClassLoader loader) {
		MainLaunchTarget target = new MainLaunchTarget(loader, MAIN_CLASS);
		launch.sessionid.set(null);
		target.args = launch.getArgs();
		return target;
	}

}
