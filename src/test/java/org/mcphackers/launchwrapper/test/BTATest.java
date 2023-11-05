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
        return "https://github.com/Better-than-Adventure/bta-download-repo/releases/download/v1.7.7.0_02/bta.jar";
    }

    @Override
    public Tweak getTweak(ClassNodeSource source, LaunchConfig config) {
        return new LegacyTweak(source, config);
    }

    @Override
    public TestFeatureBuilder getTests() {
        return new TestFeatureBuilder();
    }

}
