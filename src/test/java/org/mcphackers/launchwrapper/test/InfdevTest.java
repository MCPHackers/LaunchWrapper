package org.mcphackers.launchwrapper.test;

import org.mcphackers.launchwrapper.LaunchConfig;
import org.mcphackers.launchwrapper.tweak.LegacyTweak;
import org.mcphackers.launchwrapper.tweak.Tweak;

public class InfdevTest extends TweakTest {

    @Override
    protected String jarName() {
        return "inf-20100630-2.jar";
    }

    @Override
    protected String jarUrl() {
        return "http://files.betacraft.uk/launcher/assets/versions/inf-20100630-2.jar";
    }

    @Override
    public Tweak getTweak(LaunchConfig config) {
        return new LegacyTweak(config);
    }

    @Override
    public TestFeatureBuilder getTests() {
        return new TestFeatureBuilder()
                .tweakInfoList("LegacyTweak init", "Fix Shutdown", "LWJGL Patch", "Replace game directory", "Options load fix", "Add main");
    }
}
