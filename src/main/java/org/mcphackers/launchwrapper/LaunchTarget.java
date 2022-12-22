package org.mcphackers.launchwrapper;

import org.mcphackers.launchwrapper.loader.LaunchClassLoader;

public abstract class LaunchTarget {
	
	protected final LaunchClassLoader classLoader;
	
	public LaunchTarget(LaunchClassLoader classLoader) {
		this.classLoader = classLoader;
	}

	public abstract void launch();
}
