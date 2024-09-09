package org.mcphackers.launchwrapper.target;

import org.mcphackers.launchwrapper.loader.LaunchClassLoader;

/**
 * Invokes MainClass.main(String[] args)
 * (For example net.minecraft.client.main.Main)
 */
public class MainLaunchTarget implements LaunchTarget {

	public String targetClass;
	public String[] args;

	public MainLaunchTarget(String classTarget) {
		targetClass = classTarget.replace("/", ".");
	}

	public void launch(LaunchClassLoader classLoader) {
		classLoader.invokeMain(targetClass, args);
	}

}
