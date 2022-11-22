package org.mcphackers.launchwrapper;

public class LaunchTarget {
	public enum Type {
		MAIN,
		APPLET;
	}
	
	public Type type;
	public String targetClass;
	
	public LaunchTarget(String classTarget, Type launchType) {
		targetClass = classTarget;
		type = launchType;
	}
}
