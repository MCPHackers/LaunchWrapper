package org.mcphackers.launchwrapper.tweak;

import org.mcphackers.launchwrapper.LaunchConfig;
import org.mcphackers.launchwrapper.LaunchTarget;
import org.mcphackers.launchwrapper.inject.ClassNodeSource;
import org.mcphackers.launchwrapper.loader.LaunchClassLoader;

public abstract class Tweak {

	private static final boolean DEBUG = true;

	protected ClassNodeSource source;

	public Tweak(ClassNodeSource source) {
		this.source = source;
	}

	public abstract boolean transform();

	public abstract LaunchTarget getLaunchTarget(LaunchClassLoader loader);

	public static Tweak get(LaunchClassLoader classLoader, LaunchConfig launch) {
		if(launch.isom.get()) {
			return new IsomTweak(classLoader, launch);
		}
		if(classLoader.getClass(VanillaTweak.MAIN_CLASS) != null) {
			return new VanillaTweak(classLoader, launch);
		}
		for(String cls : LegacyTweak.MAIN_CLASSES) {
			if(classLoader.getClass(cls) != null) {
				return new LegacyTweak(classLoader, launch);
			}
		}
		for(String cls : LegacyTweak.MAIN_APPLETS) {
			if(classLoader.getClass(cls) != null) {
				return new LegacyTweak(classLoader, launch);
			}
		}
		return null; // Tweak not found
	}

	protected void debugInfo(String msg) {
		if(DEBUG)
			System.out.println("TWEAK: " + msg);
	}
}
