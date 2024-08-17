package org.mcphackers.launchwrapper.test;

import java.io.File;

import org.mcphackers.launchwrapper.Launch;
import org.mcphackers.launchwrapper.LaunchConfig;
import org.mcphackers.launchwrapper.protocol.SkinType;

public class DebugLaunch {
	public static void main(String[] args) {
        Launch.CLASS_LOADER.setDebugOutput(new File("./debug"));
		LaunchConfig config = new LaunchConfig(args);
        config.username.set("Player");
        config.session.set("-");
        config.accessToken.set("-");
        config.applet.set(false);
        config.gameDir.set(new File("./game"));
        config.fullscreen.set(true);
        config.lwjglFrame.set(true);
        config.skinProxy.set(SkinType.PRE_B1_9);
        // config.tweakClass.set("org.mcphackers.launchwrapper.tweak.LegacyTweak");
        config.resourcesProxyPort.set(11702);
		Launch.create(config).launch();
	}
    
}
