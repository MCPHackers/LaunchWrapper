package org.mcphackers.launchwrapper;

import org.mcphackers.launchwrapper.loader.LaunchClassLoader;
import org.mcphackers.launchwrapper.tweak.Tweak;

public class Launch {

	/**
	 * Class loader where overwritten classes will be stored
	 */
	public static final LaunchClassLoader CLASS_LOADER = LaunchClassLoader.instantiate();
	static {
		CLASS_LOADER.addException(Launch.class);
		CLASS_LOADER.addException(LaunchConfig.class);
		CLASS_LOADER.addException(LaunchConfig.LaunchParameter.class);
		CLASS_LOADER.addException(LaunchConfig.LaunchParameterEnum.class);
		CLASS_LOADER.addException(LaunchConfig.LaunchParameterFile.class);
		CLASS_LOADER.addException(LaunchConfig.LaunchParameterFileList.class);
		CLASS_LOADER.addException(LaunchConfig.LaunchParameterNumber.class);
		CLASS_LOADER.addException(LaunchConfig.LaunchParameterString.class);
		CLASS_LOADER.addException(LaunchConfig.LaunchParameterSwitch.class);
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
			if(config.discordRPC.get()) {
				setupDiscordRPC();
			}
			mainTweak.clear(); // Clearing some garbage
			mainTweak.getLaunchTarget().launch(CLASS_LOADER);
		} else {
			System.err.println("Tweak could not be applied");
		}
	}
	
	protected void setupDiscordRPC() {
		// TODO
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
