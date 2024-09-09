package org.mcphackers.launchwrapper.tweak;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.mcphackers.launchwrapper.Launch;
import org.mcphackers.launchwrapper.LaunchConfig;
import org.mcphackers.launchwrapper.loader.LaunchClassLoader;
import org.mcphackers.launchwrapper.target.LaunchTarget;
import org.mcphackers.launchwrapper.tweak.injection.Injection;
import org.mcphackers.launchwrapper.util.ClassNodeSource;
import org.mcphackers.launchwrapper.util.ResourceSource;

public abstract class Tweak {
	protected ClassNodeSource source;
	protected LaunchConfig config;
    private List<FeatureInfo> features = new ArrayList<FeatureInfo>();
	private boolean clean = true;

	/**
	 * Every tweak must implement this constructor
	 * Overloads are not supported, except (LaunchConfig, Tweak), where tweak parameter is the default tweak which would've been chosen otherwise 
	 * @param config
	 */
	public Tweak(LaunchConfig config) {
		this.config = config;
	}

	/**
	 * Order of injections in returned list matters!
	 * @return list of injections to apply via current tweak class.
	 */
	public abstract List<Injection> getInjections();
	
	public void transformResources(ResourceSource source) {
	}

	// Any classpath changes required for transformers can be set here
	public void prepare(LaunchClassLoader loader) {
	}
	
	public final boolean transform(ClassNodeSource source) {
		if(!clean) {
			throw new RuntimeException("Calling tweak transform twice is not allowed. Create a new instance");
		}
		clean = false;
		for(Injection injection : getInjections()) {
			boolean applied = injection.apply(source, config);
			if(applied) {
				if(injection.name() != null) {
					tweakInfo(injection.name()); // TODO extra info
				}
			}
			if(!applied && injection.required()) {
				return false;
			}
		}
		return true;
	}

	public abstract ClassLoaderTweak getLoaderTweak();

	public abstract LaunchTarget getLaunchTarget();
	
	public static Tweak get(ClassNodeSource classLoader, LaunchConfig launch) {
		if(launch.tweakClass.get() != null) {
			try {
				try {
				// Instantiate custom tweak if it's present on classpath;
				return (Tweak)Class.forName(launch.tweakClass.get())
						.getConstructor(LaunchConfig.class, Tweak.class)
						.newInstance(launch, getDefault(classLoader, launch));
				} catch (NoSuchMethodException e) {
					return (Tweak)Class.forName(launch.tweakClass.get())
							.getConstructor(LaunchConfig.class)
							.newInstance(launch);
				}
			} catch (ClassNotFoundException e) {
				return null;
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
		}
		return getDefault(classLoader, launch);
	}
	public static Tweak getDefault(ClassNodeSource classLoader, LaunchConfig launch) {
		if(launch.isom.get()) {
			return new IsomTweak(launch);
		}
		if(classLoader.getClass(VanillaTweak.MAIN_CLASS) != null) {
			return new VanillaTweak(launch);
		}
		for(String cls : LegacyTweak.MAIN_CLASSES) {
			if(classLoader.getClass(cls) != null) {
				return new LegacyTweak(launch);
			}
		}
		for(String cls : LegacyTweak.MAIN_APPLETS) {
			if(classLoader.getClass(cls) != null) {
				return new LegacyTweak(launch);
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
		Launch.LOGGER.logDebug("Applying tweak: " + name + other);
	}
}
