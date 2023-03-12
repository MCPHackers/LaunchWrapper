package org.mcphackers.launchwrapper;

import java.net.URL;

import org.mcphackers.launchwrapper.inject.Inject;
import org.mcphackers.launchwrapper.loader.LaunchClassLoader;
import org.mcphackers.launchwrapper.tweak.Tweak;

public class Launch {

	private static final LaunchClassLoader CLASS_LOADER = LaunchClassLoader.instantiate();
	public static Launch INSTANCE;

	public final LaunchConfig config;

	private Launch(LaunchConfig config) {
		this.config = config;
	}

	public static void main(String[] args) {
		LaunchConfig config = new LaunchConfig(args);
		Launch.create(config).launch();
	}

	public void launch() {
		CLASS_LOADER.addException(Launch.class);
		CLASS_LOADER.addException(Inject.class);
		Tweak mainTweak = Tweak.get(CLASS_LOADER, config);
		if(mainTweak == null) {
			System.err.println("Could not find launch target");
			return;
		}
		if(mainTweak.transform()) {
			mainTweak.getLaunchTarget(CLASS_LOADER).launch();
		}
	}

	public void setClassPath(URL[] urls) {
		CLASS_LOADER.setClassPath(urls);
	}

	public static Launch create(LaunchConfig config) {
		return INSTANCE = new Launch(config);
	}
}
