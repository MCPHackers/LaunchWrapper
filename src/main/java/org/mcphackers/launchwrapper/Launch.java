package org.mcphackers.launchwrapper;

import org.mcphackers.launchwrapper.loader.LaunchClassLoader;
import org.mcphackers.launchwrapper.tweak.Tweak;

public class Launch {

	/**
	 * Class loader where overwritten classes will be stored
	 */
	public static final String VERSION = "1.0";
	public static final LaunchClassLoader CLASS_LOADER = LaunchClassLoader.instantiate();
	static {
		CLASS_LOADER.addException("org.mcphackers.launchwrapper");
		CLASS_LOADER.addException("org.objectweb.asm");
		CLASS_LOADER.removeException("org.mcphackers.launchwrapper.inject");
	}
	protected static Launch INSTANCE;
	
	public final LaunchConfig config;

	protected Launch(LaunchConfig config) {
		this.config = config;
		INSTANCE = this;
	}

	public static void main(String[] args) {
		LaunchConfig config = new LaunchConfig(args);
		Launch.create(config).launch();
	}

	public void launch() {
		Tweak mainTweak = getTweak();
		if(mainTweak == null) {
			System.err.println("Could not find launch target");
			return;
		}
		if(mainTweak.performTransform()) {
			if(config.discordRPC.get()) {
				setupDiscordRPC();
			}
			CLASS_LOADER.setLoaderTweak(mainTweak.getLoaderTweak());
			mainTweak.getLaunchTarget().launch(CLASS_LOADER);
		} else {
			System.err.println("Tweak could not be applied");
		}
	}

	protected Tweak getTweak() {
		return Tweak.get(CLASS_LOADER, config);
	}
	
	protected void setupDiscordRPC() {
		// TODO
	}
	
	@Deprecated
	public static Launch getInstance() {
		return INSTANCE;
	}

	public static Launch create(LaunchConfig config) {
		return new Launch(config);
	}
}
