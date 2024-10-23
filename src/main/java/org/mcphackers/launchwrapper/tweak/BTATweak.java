package org.mcphackers.launchwrapper.tweak;

import java.util.Arrays;
import java.util.List;

import org.mcphackers.launchwrapper.LaunchConfig;
import org.mcphackers.launchwrapper.tweak.injection.Injection;
import org.mcphackers.launchwrapper.tweak.injection.legacy.FixShutdown;
import org.mcphackers.launchwrapper.tweak.injection.legacy.FixSplashScreen;
import org.mcphackers.launchwrapper.tweak.injection.legacy.ReplaceGameDir;

/**
 * Based on LegacyTweak but less invasive in order to maintain compatibility
 * with BTA. BTA provides some fixes so LaunchWrapper doesn't need to apply them
 */
public class BTATweak extends LegacyTweak {

	public BTATweak(LaunchConfig launch) {
		super(launch);
	}

	@Override
	public List<Injection> getInjections() {
		return Arrays.<Injection>asList(
			context,
			new FixSplashScreen(context),
			new FixShutdown(context),
			new ReplaceGameDir(context));
	}
}
