package org.mcphackers.launchwrapper.test;

import java.io.File;

import org.mcphackers.launchwrapper.Launch;
import org.mcphackers.launchwrapper.LaunchConfig;

public class DebugLaunch {
	public static void main(String[] args) {
        Launch.CLASS_LOADER.setDebugOutput(new File("./debug"));
		LaunchConfig config = new LaunchConfig(args);
        if(config.username.get() == null) {
            config.username.set("Player");
        }
        config.session.set("-");
        config.accessToken.set("-");
        Launch.create(config).launch();
	}
    
}
