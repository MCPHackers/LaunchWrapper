package org.mcphackers.launchwrapper.test;

import java.io.File;

import org.mcphackers.launchwrapper.Launch;
import org.mcphackers.launchwrapper.LaunchConfig;

public class DebugLaunch {
	public static void main(String[] args) {
		LaunchConfig config = new LaunchConfig(args);
        if(config.username.get() == null) {
            config.username.set("Player");
        }
        if(System.getProperty("java.library.path") == null) {
            System.setProperty("java.library.path", new File(config.gameDir.get(), "natives").getAbsolutePath());
        }
        config.session.set("-");
        config.accessToken.set("-");
        Launch launch = Launch.create(config);
        launch.getLoader().setDebugOutput(new File("./debug"));
        launch.launch();
	}
    
}
