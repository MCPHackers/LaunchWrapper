package org.mcphackers.launchwrapper.test;

import org.mcphackers.launchwrapper.LaunchConfig;
import org.mcphackers.launchwrapper.tweak.LegacyTweak;
import org.mcphackers.launchwrapper.tweak.Tweak;

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
	public Tweak getTweak(LaunchConfig config) {
		return new LegacyTweak(config);
	}

	@Override
	public TestFeatureBuilder getTests() {
		return new TestFeatureBuilder()
			.tweakInfoList("LegacyTweak init", "Classic crash screen", "Fix Shutdown", "Indev save patch", "LWJGL Patch", "Replace game directory", "Options load fix", "Add main");
	}
}
