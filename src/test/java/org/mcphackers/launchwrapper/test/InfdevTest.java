package org.mcphackers.launchwrapper.test;

import org.mcphackers.launchwrapper.LaunchConfig;
import org.mcphackers.launchwrapper.tweak.LegacyTweak;
import org.mcphackers.launchwrapper.tweak.Tweak;
import org.mcphackers.launchwrapper.util.ClassNodeSource;

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
    public Tweak getTweak(ClassNodeSource source, LaunchConfig config) {
        return new LegacyTweak(source, config);
    }

    @Override
    public TestFeatureBuilder getTests() {
        return new TestFeatureBuilder()
                .tweakInfoList("Set default width and height", "Fullscreen init patch", "Fullscreen toggle patch",
                        "Replaced icon", "MouseHelper fix", "Replaced fullscreen", "Replaced width",
                        "Replaced height", "Shutdown patch", "SoundManager shutdown",
                        "Options load fix", "Replaced canvas getWidth", "Replaced canvas getHeight",
                        "Replaced title", "Replaced gameDir", "Removed canvas null check", "Set fullscreen",
                        "Set default width and height", "Added main");
    }
}
