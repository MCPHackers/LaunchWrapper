package org.mcphackers.launchwrapper;

import java.io.File;

import org.mcphackers.launchwrapper.loader.LaunchClassLoader;
import org.mcphackers.launchwrapper.tweak.Tweak;

public class Launch {

	/**
	 * Class loader where overwritten classes are stored
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
		Launch.CLASS_LOADER.setDebugOutput(new File(config.gameDir.get(), "debug"));
		Tweak mainTweak = Tweak.get(CLASS_LOADER, config);
		if(mainTweak == null) {
			System.err.println("Could not find launch target");
			return;
		}
		if(mainTweak.transform()) {
			if(config.discordRPC.get()) {
				setupDiscordRPC();
			}
			mainTweak.getLaunchTarget().launch(CLASS_LOADER);
		} else {
			System.err.println("Tweak could not be applied");
		}
	}
	
	protected void setupDiscordRPC() {
		// String applicationId = "356875570916753438";
		// DiscordEventHandlers handlers = new DiscordEventHandlersAdapter() {};
		// DiscordRPC.discordInitialize(applicationId, handlers, true);
		// DiscordRichPresence presence = new DiscordRichPresence();
		// if(config.version.get() != null) {
		// 	presence.state = "Version: " + config.version.get();
		// }
		// if(config.username.get() != null) {
		// 	presence.details = "Username: " + config.username.get();
		// }
		// presence.startTimestamp = System.currentTimeMillis() / 1000;
		// presence.largeImageKey = "https://cdn.discordapp.com/app-icons/356875570916753438/166fbad351ecdd02d11a3b464748f66b.png?size=256";
		// DiscordRPC.discordUpdatePresence(presence);
		// final Thread thread =
		// new Thread("Discord RPC") {
		// 	public void run() {
		// 		while(true) {
		// 			DiscordRPC.discordRunCallbacks();
		// 			try {
		// 				Thread.sleep(2000);
		// 			} catch (InterruptedException e) {
		// 			}
		// 		}
		// 	}
		// };
		// Runtime.getRuntime().addShutdownHook(new Thread() {
		// 	public void run() {
		// 		DiscordRPC.discordShutdown();
		// 		thread.interrupt();
		// 	}
		// });
		// thread.start();
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
