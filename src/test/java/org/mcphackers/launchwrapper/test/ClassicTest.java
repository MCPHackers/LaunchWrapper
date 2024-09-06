package org.mcphackers.launchwrapper.test;

import org.mcphackers.launchwrapper.LaunchConfig;
import org.mcphackers.launchwrapper.tweak.LegacyTweak;
import org.mcphackers.launchwrapper.tweak.Tweak;

public class ClassicTest extends TweakTest {

    @Override
    protected String jarName() {
        return "c0.30_01c.jar";
    }

    @Override
    protected String jarUrl() {
        return "https://launcher.mojang.com/v1/objects/54622801f5ef1bcc1549a842c5b04cb5d5583005/client.jar";
    }

    @Override
    public Tweak getTweak(LaunchConfig config) {
        return new LegacyTweak(config);
    }

    @Override
    public TestFeatureBuilder getTests() {
        return new TestFeatureBuilder()
            .tweakInfoList("LegacyTweak init", "Fix Shutdown", "Indev save patch", "LWJGL Patch", "Replace game directory", "Add main");
    }
}
