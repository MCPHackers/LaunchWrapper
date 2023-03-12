package org.mcphackers.launchwrapper;

import org.mcphackers.launchwrapper.loader.LaunchClassLoader;

public class MainLaunchTarget extends LaunchTarget {

	public String targetClass;
	public String[] args;

	public MainLaunchTarget(LaunchClassLoader classLoader, String classTarget) {
		super(classLoader);
		targetClass = classTarget;
	}

	public void launch() {
		classLoader.invokeMain(targetClass, args);
	}

}
