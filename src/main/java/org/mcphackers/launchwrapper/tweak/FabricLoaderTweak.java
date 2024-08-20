package org.mcphackers.launchwrapper.tweak;

import org.mcphackers.launchwrapper.LaunchConfig;
import org.mcphackers.launchwrapper.LaunchTarget;
import org.mcphackers.launchwrapper.MainLaunchTarget;

public class FabricLoaderTweak extends Tweak {

    public static final String FABRIC_KNOT_CLIENT = "net/fabricmc/loader/impl/launch/knot/KnotClient";
    protected Tweak baseTweak;

    public FabricLoaderTweak(Tweak baseTweak, LaunchConfig launch) {
        super(baseTweak.source, launch);
        this.baseTweak = baseTweak;
    }

    @Override
    public boolean transform() {
        if(!baseTweak.transform()) {
            return false;
        }
        // TODO
        return true;
    }

    @Override
    public ClassLoaderTweak getLoaderTweak() {
        return baseTweak.getLoaderTweak();
    }

    @Override
    public LaunchTarget getLaunchTarget() {
        baseTweak.getLaunchTarget(); // discard target, but run any pre-launch tweaks from base tweak
        MainLaunchTarget main = new MainLaunchTarget(FABRIC_KNOT_CLIENT);
        main.args = new String[] {launch.username.get(), launch.session.get()};
        return main;
    }
    
}
