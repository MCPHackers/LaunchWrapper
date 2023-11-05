package org.mcphackers.launchwrapper.test;

import org.mcphackers.launchwrapper.LaunchConfig;
import org.mcphackers.launchwrapper.tweak.LegacyTweak;
import org.mcphackers.launchwrapper.tweak.Tweak;
import org.mcphackers.launchwrapper.util.ClassNodeSource;

public class IndevTest extends TweakTest {

    @Override
    protected String jarName() {
        return "in-20100223.jar";
    }

    @Override
    protected String jarUrl() {
        return "http://files.betacraft.uk/launcher/assets/versions/in-20100223.jar";
    }

    @Override
    public Tweak getTweak(ClassNodeSource source, LaunchConfig config) {
        return new LegacyTweak(source, config);
    }

    @Override
    public TestFeatureBuilder getTests() {
        return new TestFeatureBuilder()
                .tweakInfoList("Indev launch", "Indev save patch", "Set default width and height",
                        "Fullscreen init patch", "Replaced icon", "Replaced fullscreen", "Replaced width",
                        "Replaced height", "Shutdown patch", "Options load fix",
                        "Replaced title", "Replaced gameDir", "Removed canvas null check",
                        "Set fullscreen", "Added main");
    }

}
