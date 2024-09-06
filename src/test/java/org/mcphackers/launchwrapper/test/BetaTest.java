package org.mcphackers.launchwrapper.test;

import org.mcphackers.launchwrapper.LaunchConfig;
import org.mcphackers.launchwrapper.tweak.LegacyTweak;
import org.mcphackers.launchwrapper.tweak.Tweak;

public class BetaTest extends TweakTest {

    @Override
    protected String jarName() {
        return "b1.1_02.jar";
    }

    @Override
    protected String jarUrl() {
        return "https://launcher.mojang.com/v1/objects/e1c682219df45ebda589a557aadadd6ed093c86c/client.jar";
    }

    @Override
    public Tweak getTweak(LaunchConfig config) {
        return new LegacyTweak(config);
    }

    @Override
    public TestFeatureBuilder getTests() {
        return new TestFeatureBuilder()
            .tweakInfoList("LegacyTweak init", "Splash screen fix", "Fix Shutdown", "LWJGL Patch", "Replace game directory", "Options load fix", "Add main");
    }
}
