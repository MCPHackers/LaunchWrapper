package org.mcphackers.launchwrapper.tweak;

import java.util.ArrayList;
import java.util.List;

import org.mcphackers.launchwrapper.LaunchConfig;
import org.mcphackers.launchwrapper.target.LaunchTarget;
import org.mcphackers.launchwrapper.tweak.injection.Injection;
import org.mcphackers.launchwrapper.tweak.injection.forge.ForgeFix;

public class LegacyLauncherTweak extends Tweak {

	protected Tweak baseTweak;

	public LegacyLauncherTweak(LaunchConfig config, Tweak tweak) {
		super(config);
		baseTweak = tweak;
	}

	@Override
	public List<Injection> getInjections() {
		List<Injection> injections = new ArrayList<Injection>();
		injections.addAll(baseTweak.getInjections());
		injections.add(new ForgeFix());
		return injections;
	}

	@Override
	public List<Tweaker> getTweakers() {
		return baseTweak.getTweakers();
	}

	@Override
	public LaunchTarget getLaunchTarget() {
		return baseTweak.getLaunchTarget();
	}
}
