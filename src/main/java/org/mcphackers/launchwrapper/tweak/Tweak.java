package org.mcphackers.launchwrapper.tweak;

import org.mcphackers.launchwrapper.LaunchConfig;
import org.mcphackers.launchwrapper.LaunchTarget;
import org.mcphackers.launchwrapper.loader.LaunchClassLoader;
import org.mcphackers.launchwrapper.util.ClassNodeSource;

public abstract class Tweak {

	private static final boolean DEBUG = false;

	protected ClassNodeSource source;
	protected LaunchConfig launch;

	public Tweak(ClassNodeSource source, LaunchConfig launch) {
		this.source = source;
		this.launch = launch;
	}

	public abstract boolean transform();

	public abstract ClassLoaderTweak getLoaderTweak();

	public abstract LaunchTarget getLaunchTarget();

	public static Tweak get(LaunchClassLoader classLoader, LaunchConfig launch) {
		Tweak tweak = getTweak(classLoader, launch);
		if(tweak != null) {
			classLoader.setLoaderTweak(tweak.getLoaderTweak());
		}
		return tweak;
	}
	
	private static Tweak getTweak(LaunchClassLoader classLoader, LaunchConfig launch) {
		if(launch.tweakClass.get() != null) {
			try {
				return (Tweak)Class.forName(launch.tweakClass.get())
						.getConstructor(ClassNodeSource.class, LaunchConfig.class)
						.newInstance(classLoader, launch);
			} catch (ClassNotFoundException e) {
				return null;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
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

	protected void tweakInfo(String msg) {
		if(DEBUG)
			System.out.println("TWEAK: " + msg);
	}
}
