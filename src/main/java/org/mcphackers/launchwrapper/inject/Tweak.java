package org.mcphackers.launchwrapper.inject;

import org.mcphackers.launchwrapper.loader.LaunchClassLoader;

public abstract class Tweak {
	
	protected ClassNodeSource source;
	
	public Tweak(LaunchClassLoader source) {
		this.source = source;
		source.addException(getClass());
	}
	
	public abstract boolean transform();
	
	public abstract String getLaunchTarget();
}
