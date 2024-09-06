package org.mcphackers.launchwrapper.test;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mcphackers.launchwrapper.LaunchConfig;
import org.mcphackers.launchwrapper.tweak.FeatureInfo;
import org.mcphackers.launchwrapper.tweak.Tweak;
import org.mcphackers.launchwrapper.util.Util;

public abstract class TweakTest {
    
    @Test
    public void test() {
        try {
            File testDir = new File("build/test/");
            File gameJar = new File(testDir, jarName());
            downloadMinecraftJar(
                    jarUrl(),
                    gameJar);
            FileClassNodeSource classNodeSource = new FileClassNodeSource(gameJar);
            LaunchConfig config = getDefaultConfig(testDir);
            Tweak tweak = getTweak(config);
            assertTrue(tweak.transform(classNodeSource));
            System.out.println(tweak.getClass().toString());
            for (FeatureInfo info : tweak.getTweakInfo()) {
                System.out.println(info.feature);
            }
            featureTest(tweak, getTests());
        } catch (Throwable e) {
            fail(e);
        }
    }

    protected abstract String jarName();

    protected abstract String jarUrl();

    public abstract Tweak getTweak(LaunchConfig config);

    public abstract TestFeatureBuilder getTests();

    protected final void downloadMinecraftJar(String url, File outFile) throws IOException {
        if (outFile.exists()) {
            return;
        }
        outFile.getParentFile().mkdirs();
        Util.copyStream(new URL(url).openStream(), new FileOutputStream(outFile));
    }

    protected static final LaunchConfig getDefaultConfig(File testDir) {
        LaunchConfig config = new LaunchConfig();
        File icon16 = new File("src/main/resources/icon_16x16.png");
        File icon32 = new File("src/main/resources/icon_32x32.png");
        config.icon.set(new File[] { icon16, icon32 });
        config.username.set("Player");
        config.session.set("-");
        config.gameDir.set(testDir);
        config.title.set("Minecraft Test");
        return config;
    }

    protected void featureTest(Tweak tweak, TestFeatureBuilder test) {
        try {
            for (FeatureInfo featureInfo2 : test.bannedFeatures) {
                for (FeatureInfo featureInfo : tweak.getTweakInfo()) {
                    if (featureInfo.feature.equals(featureInfo2.feature) &&
                            (featureInfo2.info == null || featureInfo2.info.equals(featureInfo.info))) {
                        fail("Banned feature found");
                    }
                }
            }
            for (FeatureInfo featureInfo2 : test.features) {
                boolean match = false;
                for (FeatureInfo featureInfo : tweak.getTweakInfo()) {
                    if (featureInfo.feature.equals(featureInfo2.feature) &&
                            (featureInfo2.info == null || featureInfo2.info.equals(featureInfo.info))) {
                        match = true;
                    }
                }
                assertTrue(match, featureInfo2.feature);
            }
        } catch (Throwable e) {
            fail(e);
        }
    }

    public static class TestFeatureBuilder {
        private List<FeatureInfo> features = new ArrayList<FeatureInfo>();
        private List<FeatureInfo> bannedFeatures = new ArrayList<FeatureInfo>();

        public TestFeatureBuilder() {
        }

        public TestFeatureBuilder tweakInfo(String name, String... extra) {
            features.add(new FeatureInfo(name));
            return this;
        }

        public TestFeatureBuilder tweakInfoBan(String name, String... extra) {
            bannedFeatures.add(new FeatureInfo(name));
            return this;
        }

        public TestFeatureBuilder tweakInfoList(String... names) {
            for (String s : names) {
                features.add(new FeatureInfo(s));
            }
            return this;
        }

        public TestFeatureBuilder tweakInfoBanList(String... names) {
            for (String s : names) {
                bannedFeatures.add(new FeatureInfo(s));
            }
            return this;
        }
    }
}
