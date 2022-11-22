package org.mcphackers.launchwrapper.tweak;

import org.mcphackers.launchwrapper.LaunchTarget;
import org.mcphackers.launchwrapper.inject.ClassNodeSource;

public abstract class Tweak {
	
	protected ClassNodeSource source;
	
	public Tweak(ClassNodeSource source) {
		this.source = source;
	}
	
	public abstract boolean transform();
	
	public abstract LaunchTarget getLaunchTarget();
}
