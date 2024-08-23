package org.mcphackers.launchwrapper.tweak;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.mcphackers.launchwrapper.LaunchConfig;
import org.mcphackers.launchwrapper.LaunchTarget;
import org.mcphackers.launchwrapper.util.ClassNodeSource;

public abstract class Tweak {
	private static final boolean LOG_TWEAKS = Boolean.parseBoolean(System.getProperty("launchwrapper.log", "false"));

	protected ClassNodeSource source;
	protected LaunchConfig launch;
    private List<FeatureInfo> features = new ArrayList<FeatureInfo>();
	private boolean clean = true;

	/**
	 * Every tweak must implement this constructor
	 * Overloads are not supported!!!
	 * @param source
	 * @param launch
	 */
	public Tweak(ClassNodeSource source, LaunchConfig launch) {
		this.source = source;
		this.launch = launch;
	}

	/**
	 * This method does return true even if some of the changes weren't applied, even when they should've been
	 * @return true if given ClassNodeSource was modified without fatal errors
	 */
	protected abstract boolean transform();
	
	public boolean performTransform() {
		if(!clean) {
			throw new RuntimeException("Calling tweak transform twice is not allowed. Create a new instance");
		}
		clean = false;
		return transform();
	}

	public abstract ClassLoaderTweak getLoaderTweak();

	public abstract LaunchTarget getLaunchTarget();
	
	public static Tweak get(ClassNodeSource classLoader, LaunchConfig launch) {
		if(launch.tweakClass.get() != null) {
			try {
				// Instantiate custom tweak if it's present on classpath;
				return (Tweak)Class.forName(launch.tweakClass.get())
						.getConstructor(ClassNodeSource.class, LaunchConfig.class)
						.newInstance(classLoader, launch);
			} catch (ClassNotFoundException e) {
				return null;
			} catch (Exception e) {
				e.printStackTrace();
				return null;
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

	public List<FeatureInfo> getTweakInfo() {
		return Collections.unmodifiableList(features);
	}

	protected void tweakInfo(String name, String... extra) {
		features.add(new FeatureInfo(name));
		StringBuilder other = new StringBuilder();
		for(String s : extra) {
			other.append(" ").append(s);
		}
		if(LOG_TWEAKS) {
			System.out.println("[LaunchWrapper] Applying tweak: " + name + other);
		}
	}
}
