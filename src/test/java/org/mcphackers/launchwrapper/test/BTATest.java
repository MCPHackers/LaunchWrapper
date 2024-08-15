package org.mcphackers.launchwrapper.test;

import org.mcphackers.launchwrapper.LaunchConfig;
import org.mcphackers.launchwrapper.tweak.LegacyTweak;
import org.mcphackers.launchwrapper.tweak.Tweak;
import org.mcphackers.launchwrapper.util.ClassNodeSource;

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
    public Tweak getTweak(ClassNodeSource source, LaunchConfig config) {
        return new LegacyTweak(source, config);
    }

    @Override
    public TestFeatureBuilder getTests() {
        return new TestFeatureBuilder()
                .tweakInfoList("Shutdown patch", "Replaced gameDir", "Set initial width", "Set initial height");
    }

}
