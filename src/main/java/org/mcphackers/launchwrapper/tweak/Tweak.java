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

import net.minecraft.launchwrapper.ITweaker;

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

	public boolean handleError(LaunchClassLoader loader, Throwable t) {
		if(t != null) {
			t.printStackTrace();
		}
		return false;
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

	public abstract List<LazyTweaker> getLazyTweakers();

	public abstract LaunchTarget getLaunchTarget();

	public static Tweak forClass(ClassNodeSource source, LaunchConfig launch, String clazz) {
		try {
			Class<?> cls = Class.forName(clazz);
			if(Tweak.class.isAssignableFrom(cls)) {
				Class<? extends Tweak> tweak = cls.asSubclass(Tweak.class);
				try {
					return tweak
						.getConstructor(LaunchConfig.class, Tweak.class)
						.newInstance(launch, getDefault(source, launch));
				} catch (NoSuchMethodException e) {
					return tweak
							.getConstructor(LaunchConfig.class)
							.newInstance(launch);
				}
			}
			else if(ITweaker.class.isAssignableFrom(cls)) {
				return new LegacyLauncherTweak(launch, getDefault(source, launch));
			} else {
				return null;
			}
		} catch (ClassNotFoundException e) {
			return null;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public static Tweak get(ClassNodeSource classLoader, LaunchConfig launch) {
		if(launch.tweakClass.get() != null) {
			return forClass(classLoader, launch, launch.tweakClass.get());
		}
		return getDefault(classLoader, launch);
	}
	public static Tweak getDefault(ClassNodeSource classLoader, LaunchConfig launch) {
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
		for(String cls : ServerTweak.MAIN_CLASSES) {
			if(classLoader.getClass(cls) != null) {
				return new ServerTweak(launch);
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
