package org.mcphackers.launchwrapper.test;

import org.mcphackers.launchwrapper.LaunchConfig;
import org.mcphackers.launchwrapper.tweak.BTATweak;
import org.mcphackers.launchwrapper.tweak.Tweak;

public class BTATest extends TweakTest {

    @Override
    protected String jarName() {
        return "bta.jar";
    }

    @Override
    protected String jarUrl() {
        return "https://github.com/Better-than-Adventure/bta-download-repo/releases/download/v7.2_01/bta-7.2_01-client.jar";
    }

    @Override
    public Tweak getTweak(LaunchConfig config) {
        return new BTATweak(config);
    }

    @Override
    public TestFeatureBuilder getTests() {
        return new TestFeatureBuilder()
            .tweakInfoList("LegacyTweak init", "Fix Shutdown", "Replace game directory");
    }

}
