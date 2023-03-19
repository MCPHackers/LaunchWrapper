package org.mcphackers.launchwrapper;

import org.mcphackers.launchwrapper.inject.Inject;
import org.mcphackers.launchwrapper.loader.LaunchClassLoader;
import org.mcphackers.launchwrapper.tweak.Tweak;

public class Launch {

	/**
	 * Class loader where overwritten classes are stored
	 */
	public static final LaunchClassLoader CLASS_LOADER = LaunchClassLoader.instantiate();
	static {
		CLASS_LOADER.addException(Launch.class);
		CLASS_LOADER.addException(Inject.class);
	}
	private static Launch INSTANCE;
	
	public final LaunchConfig config;

	private Launch(LaunchConfig config) {
		this.config = config;
	}

	public static void main(String[] args) {
		LaunchConfig config = new LaunchConfig(args);
		Launch.create(config).launch();
	}

	public void launch() {
		Tweak mainTweak = Tweak.get(CLASS_LOADER, config);
		if(mainTweak == null) {
			System.err.println("Could not find launch target");
			return;
		}
		if(mainTweak.transform()) {
			mainTweak.getLaunchTarget().launch(CLASS_LOADER);
		} else {
			System.err.println("Tweak could not be applied");
		}
	}
	
	public static Launch getInstance() {
		return INSTANCE;
	}
	
	public static LaunchConfig getConfig() {
		return INSTANCE.config;
	}

	public static Launch create(LaunchConfig config) {
		return INSTANCE = new Launch(config);
	}
}
