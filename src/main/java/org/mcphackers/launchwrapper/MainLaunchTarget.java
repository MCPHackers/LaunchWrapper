package org.mcphackers.launchwrapper;

import org.mcphackers.launchwrapper.loader.LaunchClassLoader;

public class MainLaunchTarget extends LaunchTarget {

	public String targetClass;
	public String[] args;

	public MainLaunchTarget(String classTarget) {
		targetClass = classTarget;
	}

	public void launch(LaunchClassLoader classLoader) {
		classLoader.invokeMain(targetClass, args);
	}

}
