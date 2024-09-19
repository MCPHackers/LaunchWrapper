package org.mcphackers.launchwrapper.target;

import org.mcphackers.launchwrapper.loader.LaunchClassLoader;

/**
 * Invokes MainClass.main(String[] args)
 * (For example net.minecraft.client.main.Main)
 */
public class MainLaunchTarget implements LaunchTarget {

	public final String targetClass;
	public final String[] args;

	public MainLaunchTarget(String classTarget, String[] arguments) {
		targetClass = classTarget.replace("/", ".");
		args = arguments;
	}

	public void launch(LaunchClassLoader classLoader) {
		classLoader.invokeMain(targetClass, args);
	}

}
