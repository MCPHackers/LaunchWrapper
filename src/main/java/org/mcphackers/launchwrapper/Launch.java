package org.mcphackers.launchwrapper;

import org.mcphackers.launchwrapper.loader.LaunchClassLoader;
import org.mcphackers.launchwrapper.target.LaunchTarget;
import org.mcphackers.launchwrapper.tweak.Tweak;

public class Launch {

	public static final String VERSION = "1.1.3";
	public static final Logger LOGGER = new Logger();

	/**
	 * Class loader where overwritten classes will be stored
	 */
	private static LaunchClassLoader CLASS_LOADER;
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
		if (mainTweak == null) {
			if (config.tweakClass.get() == null) {
				LOGGER.logErr("No suitable tweak found. Is Minecraft on classpath?");
			} else {
				LOGGER.logErr("Specified tweak does not exist.");
			}
			return;
		}
		mainTweak.prepare(loader);
		try {
			if (!mainTweak.transform(loader)) {
				LOGGER.logErr("Tweak could not be applied");
				if (!mainTweak.handleError(loader, null)) { // if handled successfully, continue
					return;
				}
			}
		} catch (Throwable t) {
			LOGGER.logErr("Tweak could not be applied", t);
			if (!mainTweak.handleError(loader, t)) {
				return;
			}
		}
		mainTweak.transformResources(loader);
		LaunchTarget target = mainTweak.getLaunchTarget();
		if (target == null) {
			LOGGER.logErr("Could not find launch target");
			return;
		}
		loader.setLoaderTweakers(mainTweak.getTweakers());
		target.launch(loader);
	}

	protected Tweak getTweak() {
		return Tweak.get(getLoader(), config);
	}

	protected LaunchClassLoader getLoader() {
		if (CLASS_LOADER == null) {
			CLASS_LOADER = LaunchClassLoader.instantiate();
			CLASS_LOADER.setDebugOutput(config.debugClassDump.get());
		}
		return CLASS_LOADER;
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

		public void logErr(String msg, Throwable e) {
			System.err.println("[LaunchWrapper] " + msg);
			e.printStackTrace(System.err);
		}

		public void logDebug(String format, Object... args) {
			if (!DEBUG) {
				return;
			}
			System.out.println("[LaunchWrapper] " + String.format(format, args));
		}
	}
}
