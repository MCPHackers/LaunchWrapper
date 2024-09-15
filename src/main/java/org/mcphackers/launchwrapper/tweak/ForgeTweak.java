package org.mcphackers.launchwrapper.tweak;

import java.util.ArrayList;
import java.util.List;

import org.mcphackers.launchwrapper.LaunchConfig;
import org.mcphackers.launchwrapper.tweak.injection.Injection;
import org.mcphackers.launchwrapper.tweak.injection.legacy.ForgeVersionCheck;

public class ForgeTweak extends LegacyTweak {

    public ForgeTweak(LaunchConfig config) {
        super(config);
    }

    public List<Injection> getInjections() {
		List<Injection> list = new ArrayList<Injection>();
        list.addAll(super.getInjections());
        list.add(new ForgeVersionCheck());
        return list;
	}

}
