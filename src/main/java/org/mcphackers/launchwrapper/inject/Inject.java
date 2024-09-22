package org.mcphackers.launchwrapper.inject;

import java.io.File;
import java.io.IOException;

import org.mcphackers.launchwrapper.Launch;
import org.mcphackers.launchwrapper.protocol.SaveRequests;
import org.mcphackers.launchwrapper.tweak.AppletWrapper;
import org.mcphackers.launchwrapper.tweak.injection.legacy.IndevSaving;

/**
 * Static utility methods for injections
 */
public final class Inject {
	private Inject() {
	}

	public static AppletWrapper getApplet() {
		return new AppletWrapper(Launch.getInstance().config.getArgsAsMap());
	}

	/**
	 * Used in level load screen {@link IndevSaving}
	 */
	public static File saveLevel(int index, String name) {
		File levelsDir = new File(Launch.getInstance().config.gameDir.get(), "levels");
		String[] levels;
		try {
			levels = SaveRequests.getLevelNames(levelsDir);
			levels[index] = name;
			SaveRequests.updateLevelNames(levelsDir, levels);
		} catch (IOException e) {
			Launch.LOGGER.logErr("Failed to save", e);
		}
		return getLevelFile(index);
	}

	/**
	 * Used in level save screen {@link IndevSaving}
	 */
	public static File getLevelFile(int index) {
		return new File(Launch.getInstance().config.gameDir.get(), "levels/level" + index + ".dat");
	}
}
