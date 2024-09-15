package org.mcphackers.launchwrapper.tweak;

import java.util.ArrayList;
import java.util.List;

import org.mcphackers.launchwrapper.LaunchConfig;
import org.mcphackers.launchwrapper.target.LaunchTarget;
import org.mcphackers.launchwrapper.tweak.injection.Injection;
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
        injects.addAll(baseTweak.getInjections());
        injects.add(new FabricHook());
        return injects;
	}

    @Override
    public List<LazyTweaker> getLazyTweakers() {
        return baseTweak.getLazyTweakers();
    }

    @Override
    public LaunchTarget getLaunchTarget() {
        return baseTweak.getLaunchTarget();
    }
    
}
