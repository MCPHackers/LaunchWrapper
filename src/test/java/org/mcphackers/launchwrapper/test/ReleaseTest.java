package org.mcphackers.launchwrapper.test;

import org.mcphackers.launchwrapper.LaunchConfig;
import org.mcphackers.launchwrapper.tweak.Tweak;
import org.mcphackers.launchwrapper.tweak.VanillaTweak;
import org.mcphackers.launchwrapper.util.ClassNodeSource;

public class ReleaseTest extends TweakTest {

    @Override
    protected String jarName() {
        return "1.12.2.jar";
    }

    @Override
    protected String jarUrl() {
        return "https://piston-data.mojang.com/v1/objects/0f275bc1547d01fa5f56ba34bdc87d981ee12daf/client.jar";
    }

    @Override
    public Tweak getTweak(ClassNodeSource source, LaunchConfig config) {
        return new VanillaTweak(source, config);
    }

    @Override
    public TestFeatureBuilder getTests() {
        return new TestFeatureBuilder()
                .tweakInfoList("Minecraft.run()", "Replaced title", "Replaced icon");
    }
}
