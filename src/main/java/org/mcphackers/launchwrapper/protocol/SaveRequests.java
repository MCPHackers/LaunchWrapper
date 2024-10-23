package org.mcphackers.launchwrapper.protocol;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.mcphackers.launchwrapper.util.Util;

public final class SaveRequests {
	public static final String EMPTY_LEVEL = "-";
	public static final int MAX_LEVELS = 5;

	private SaveRequests() {
	}

	public static String[] getLevelNames(File levelsDir) throws IOException {
		String[] lvlNames = new String[MAX_LEVELS];
		for (int i = 0; i < MAX_LEVELS; i++) {
			lvlNames[i] = EMPTY_LEVEL;
			File level = new File(levelsDir, "level" + i + ".dat");
			if (level.isFile()) {
				lvlNames[i] = "Unnamed level";
			}
		}
		File levelNames = new File(levelsDir, "levels.txt");
		if (!levelNames.exists()) {
			return lvlNames;
		}
		FileInputStream levelNamesStream = new FileInputStream(levelNames);
		try {
			String[] fileStrings = new String(Util.readStream(levelNamesStream)).split(";");
			for (int i = 0; i < MAX_LEVELS && i < fileStrings.length; i++) {
				File level = new File(levelsDir, "level" + i + ".dat");
				if (level.isFile()) {
					lvlNames[i] = fileStrings[i];
				}
			}
		} finally {
			levelNamesStream.close();
		}
		return lvlNames;
	}

	public static void updateLevelNames(File levelsDir, String[] lvlNames) throws IOException {
		StringBuilder lvls = new StringBuilder();
		for (int i = 0; i < MAX_LEVELS; i++) {
			lvls.append(lvlNames[i]).append(";");
		}
		byte[] lvlsData = lvls.toString().getBytes();
		FileOutputStream outputNames = new FileOutputStream(new File(levelsDir, "levels.txt"));
		try {
			outputNames.write(lvlsData);
		} finally {
			outputNames.close();
		}
	}

	public static byte[] loadLevel(File levelsDir, int index) throws IOException {
		File level = new File(levelsDir, "level" + index + ".dat");
		if (!level.exists()) {
			throw new FileNotFoundException("Level doesn't exist");
		}
		FileInputStream in = new FileInputStream(level);
		return Util.readStream(in);
	}

	/**
	 * Used in level save screen
	 */
	public static void saveLevel(File levelsDir, int index, String levelName, byte[] data) throws IOException {
		String[] lvlNames = getLevelNames(levelsDir);
		lvlNames[index] = levelName;
		File level = new File(levelsDir, "level" + index + ".dat");

		// Since minecraft doesn't have a delete button on levels, just save a new one with this name and it'll get deleted
		if (levelName.equals("---") /* Can't be "-" because save button is active at 3>= characters */) {
			level.delete();
			lvlNames[index] = EMPTY_LEVEL;
		} else {
			if (!levelsDir.exists()) {
				levelsDir.mkdirs();
			}
			OutputStream fileOutput = new FileOutputStream(level);
			try {
				fileOutput.write(data);
			} finally {
				fileOutput.close();
			}
		}
		updateLevelNames(levelsDir, lvlNames);
	}
}
