package org.mcphackers.launchwrapper.test;

import java.io.File;

import org.mcphackers.launchwrapper.Launch;
import org.mcphackers.launchwrapper.LaunchConfig;
import org.mcphackers.launchwrapper.loader.LaunchClassLoader;

public final class DebugLaunch extends Launch {
	private DebugLaunch(LaunchConfig config) {
		super(config);
	}

	@Override
	protected LaunchClassLoader getLoader() {
		LaunchClassLoader loader = super.getLoader();
		loader.setDebugOutput(new File("./debug"));
		return loader;
	}

	public static void main(String[] args) {
		LaunchConfig config = new LaunchConfig(args);
		if (System.getProperty("java.library.path") == null) {
			System.setProperty("java.library.path", new File(config.gameDir.get(), "natives").getAbsolutePath());
		}
		System.setProperty("launchwrapper.log", "true");
		Launch launch = new DebugLaunch(config);
		launch.launch();
	}
}
