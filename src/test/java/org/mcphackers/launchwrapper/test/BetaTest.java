package org.mcphackers.launchwrapper.test;

import org.mcphackers.launchwrapper.LaunchConfig;
import org.mcphackers.launchwrapper.tweak.LegacyTweak;
import org.mcphackers.launchwrapper.tweak.Tweak;
import org.mcphackers.launchwrapper.util.ClassNodeSource;

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
    public Tweak getTweak(ClassNodeSource source, LaunchConfig config) {
        return new LegacyTweak(source, config);
    }

    @Override
    public TestFeatureBuilder getTests() {
        return new TestFeatureBuilder()
                .tweakInfoList("Set default width and height", "Fullscreen init patch", "Fullscreen toggle patch",
                        "Replaced icon", "MouseHelper fix", "Replaced fullscreen", "Replaced width",
                        "Replaced height", "Splash fix", "Shutdown patch", "SoundManager shutdown",
                        "Options load fix", "Replaced canvas getWidth", "Replaced canvas getHeight",
                        "Replaced title", "Replaced gameDir", "Removed canvas null check", "Added main");
    }
}
