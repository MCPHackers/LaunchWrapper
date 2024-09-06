package org.mcphackers.launchwrapper.tweak;

import java.util.Arrays;
import java.util.List;

import org.mcphackers.launchwrapper.LaunchConfig;
import org.mcphackers.launchwrapper.tweak.injection.Injection;
import org.mcphackers.launchwrapper.tweak.injection.legacy.FixShutdown;
import org.mcphackers.launchwrapper.tweak.injection.legacy.LegacyInit;
import org.mcphackers.launchwrapper.tweak.injection.legacy.ReplaceGameDir;
import org.mcphackers.launchwrapper.tweak.injection.legacy.SplashScreenFix;

public class BTATweak extends LegacyTweak {

	public BTATweak(LaunchConfig launch) {
		super(launch);
	}

    @Override
	public List<Injection> getInjections() {
		return Arrays.<Injection>asList(
			new LegacyInit(context),
			new SplashScreenFix(context),
			new FixShutdown(context),
			// new LWJGLPatch(context),
			new ReplaceGameDir(context)
		);
	}
    
}
