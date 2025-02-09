package org.mcphackers.launchwrapper.tweak;

import java.util.ArrayList;
import java.util.List;

import org.mcphackers.launchwrapper.LaunchConfig;
import org.mcphackers.launchwrapper.target.LaunchTarget;
import org.mcphackers.launchwrapper.tweak.injection.Injection;
import org.mcphackers.launchwrapper.tweak.injection.MinecraftGetter;
import org.mcphackers.launchwrapper.tweak.injection.fabric.FabricHook;

public class FabricLoaderTweak extends Tweak {

	protected Tweak baseTweak;

	public FabricLoaderTweak(Tweak baseTweak, LaunchConfig launch) {
		super(launch);
		this.baseTweak = baseTweak;
	}

	@Override
	public List<Injection> getInjections() {
		List<Injection> injects = new ArrayList<Injection>();
		List<Injection> baseInjections = baseTweak.getInjections();
		injects.addAll(baseInjections);
		for (Injection injection : baseInjections) {
			if (injection instanceof MinecraftGetter) {
				injects.add(new FabricHook((MinecraftGetter)injection));
				break;
			}
		}
		return injects;
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
