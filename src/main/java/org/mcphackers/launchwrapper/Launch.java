package org.mcphackers.launchwrapper;

import org.mcphackers.launchwrapper.loader.LaunchClassLoader;
import org.mcphackers.launchwrapper.target.LaunchTarget;
import org.mcphackers.launchwrapper.tweak.Tweak;

public class Launch {

	public static final String VERSION = "1.0";
	public static final Logger LOGGER = new Logger();
	
	/**
	 * Class loader where overwritten classes will be stored
	 */
	private static final LaunchClassLoader CLASS_LOADER = LaunchClassLoader.instantiate();
	static {
		CLASS_LOADER.addException("org.mcphackers.launchwrapper");
		CLASS_LOADER.addException("org.objectweb.asm");
		CLASS_LOADER.removeException("org.mcphackers.launchwrapper.inject");
	}
	private static Launch INSTANCE;
	
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
		LaunchClassLoader loader = getLoader();
		Tweak mainTweak = getTweak();
		if(mainTweak == null) {
			if(config.tweakClass.get() == null) {
				LOGGER.logErr("No suitable tweak found. Is Minecraft on classpath?");
			} else {
				LOGGER.logErr("Specified tweak does not exist.");
			}
			return;
		}
		mainTweak.prepare(loader);
		if(mainTweak.transform(loader)) {
			mainTweak.transformResources(loader);
			if(config.discordRPC.get()) {
				setupDiscordRPC();
			}
			loader.setLoaderTweakers(mainTweak.getLazyTweakers());
			LaunchTarget target = mainTweak.getLaunchTarget();
			if(target != null) {
				target.launch(loader);
			} else {
				LOGGER.logErr("Could not find launch target");
			}
		} else {
			LOGGER.logErr("Tweak could not be applied");
		}
	}

	protected Tweak getTweak() {
		return Tweak.get(CLASS_LOADER, config);
	}

	public LaunchClassLoader getLoader() {
		return CLASS_LOADER;
	}
	
	protected void setupDiscordRPC() {
		// TODO
	}
	
	public static Launch getInstance() {
		return INSTANCE;
	}

	public static Launch create(LaunchConfig config) {
		return new Launch(config);
	}

	public static class Logger {
		private static final boolean DEBUG = Boolean.parseBoolean(System.getProperty("launchwrapper.log", "false"));
		
		public void log(String format, Object... args) {
			System.out.println("[LaunchWrapper] " + String.format(format, args));
		}

		public void logErr(String format, Object... args) {
			System.err.println("[LaunchWrapper] " + String.format(format, args));
		}

		public void logDebug(String format, Object... args) {
			if(!DEBUG) {
				return;
			}
			System.out.println("[LaunchWrapper] " + String.format(format, args));
		}
	}
}
