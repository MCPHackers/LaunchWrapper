package org.mcphackers.launchwrapper.tweak;

import java.io.IOException;

import javax.imageio.ImageIO;

import org.mcphackers.launchwrapper.AppletLaunchTarget;
import org.mcphackers.launchwrapper.Launch;
import org.mcphackers.launchwrapper.LaunchConfig;
import org.mcphackers.launchwrapper.LaunchTarget;

public class IsomTweak extends LegacyTweak {
	public static final String MAIN_ISOM = "net/minecraft/isom/IsomPreviewApplet";


	public IsomTweak(LaunchConfig launch) {
		super(launch);
	}

	public LaunchTarget getLaunchTarget() {
		AppletLaunchTarget launchTarget = new AppletLaunchTarget(IsomTweak.MAIN_ISOM.replace("/", "."));
		if(config.title.get() != null) {
			launchTarget.setTitle(config.title.get());
		} else {
			launchTarget.setTitle("IsomPreview");
		}
		try {
			launchTarget.setIcon(ImageIO.read(Launch.class.getClassLoader().getResourceAsStream("/favicon.png")));
		} catch (IOException e) {
			e.printStackTrace();
		}
		launchTarget.setResolution(config.width.get(), config.height.get());
		return launchTarget;
	}

}
