package org.mcphackers.launchwrapper.test;

import org.mcphackers.launchwrapper.LaunchConfig;
import org.mcphackers.launchwrapper.tweak.LegacyTweak;
import org.mcphackers.launchwrapper.tweak.Tweak;

public class AlphaTest extends TweakTest {

    @Override
    protected String jarName() {
        return "a1.1.1.jar";
    }

    @Override
    protected String jarUrl() {
        return "http://files.betacraft.uk/launcher/assets/versions/a1.1.1.jar";
    }

    @Override
    public Tweak getTweak(LaunchConfig config) {
        return new LegacyTweak(config);
    }

    @Override
    public TestFeatureBuilder getTests() {
        return new TestFeatureBuilder()
                .tweakInfoList("LegacyTweak init", "Classic crash screen", "Fix Shutdown", "LWJGL Patch", "Splash screen fix", "Replace game directory", "Options load fix", "Fix gray screen", "Add main");
    }
}
