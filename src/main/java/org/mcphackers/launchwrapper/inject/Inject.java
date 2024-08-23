package org.mcphackers.launchwrapper.inject;

import static org.mcphackers.launchwrapper.protocol.ListLevelsURLConnection.EMPTY_LEVEL;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import org.mcphackers.launchwrapper.Launch;
import org.mcphackers.launchwrapper.tweak.AppletWrapper;
import org.mcphackers.launchwrapper.util.Util;

/**
 * DO NOT use this class under any circumstances. This class is meant to be
 * initialized in the context of LaunchClassLoader
 * This class contains methods which are used in different game classes, sort of like mixins
 * 
 * @author Lassebq
 *
 */
public final class Inject {
	private Inject() {
	}

	public static AppletWrapper getApplet() {
		return new AppletWrapper(Launch.getInstance().config.getArgsAsMap());
	}

	/**
	 * Indev load level injection
	 */
	public static File getLevelFile(int index) {
		return new File(Launch.getInstance().config.gameDir.get(), "levels/level" + index + ".dat");
	}

	/**
	 * Indev save level injection
	 */
	public static File saveLevel(int index, String levelName) {
		final int maxLevels = 5;
		File levels = new File(Launch.getInstance().config.gameDir.get(), "levels");
		File level = new File(levels, "level" + index + ".dat");
		File levelNames = new File(levels, "levels.txt");
		String[] lvlNames = new String[maxLevels];
		for(int i = 0; i < maxLevels; i++) {
			lvlNames[i] = EMPTY_LEVEL;
		}
		if(levelNames.exists()) {
			try {
				FileInputStream levelNamesStream = new FileInputStream(levelNames);
				lvlNames = new String(Util.readStream(levelNamesStream)).split(";");
				levelNamesStream.close();
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			}
		}
		lvlNames[index] = levelName;
		// Since minecraft doesn't have a delete button on levels, just save a new one with this name and it'll get deleted
		if(levelName.equals("---")) {
			level.delete();
			level = null;
			lvlNames[index] = EMPTY_LEVEL;
		}
		if(!levels.exists()) {
			levels.mkdirs();
		}
		try {
			FileOutputStream outputNames = new FileOutputStream(levelNames);
			String lvls = "";
			for(int i = 0; i < maxLevels; i++) {
				lvls += lvlNames[i] + ";";
			}
			outputNames.write(lvls.getBytes());
			outputNames.close();
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		return level;
	}

	private static ByteBuffer loadIcon(BufferedImage icon) {
		final int[] rgb = icon.getRGB(0, 0, icon.getWidth(), icon.getHeight(), null, 0, icon.getWidth());

		final ByteBuffer buffer = ByteBuffer.allocate(4 * rgb.length);
		for(int color : rgb) {
			buffer.putInt(color << 8 | ((color >> 24) & 0xFF));
		}
		buffer.flip();
		return buffer;
	}

	public static ByteBuffer[] loadIcon(boolean favIcon) {
		File[] iconPaths = Launch.getInstance().config.icon.get();
		if(iconPaths != null && hasIcon(iconPaths)) {
			List<ByteBuffer> processedIcons = new ArrayList<ByteBuffer>();
			for(File icon : iconPaths) {
				if(!icon.exists()) {
					continue;
				}
				try {
					processedIcons.add(loadIcon(ImageIO.read(icon)));
				} catch (IOException e) {
					System.err.println("Could not load icon at: " + icon.getAbsolutePath());
				}
			}
			return processedIcons.toArray(new ByteBuffer[0]);
		}
		if(!favIcon) {
			try {
				return new ByteBuffer[] { loadIcon("/icon_16x16.png"), loadIcon("/icon_32x32.png") };
			} catch (IOException e) {
				System.err.println("Could not load the default icon");
			}
		}
		try {
			return new ByteBuffer[] { loadIcon("/favicon.png") };
		} catch (IOException e) {
			System.err.println("Could not load favicon.png");
		}
		return null;
	}

	private static boolean hasIcon(File[] icons) {
		for(File f : icons) {
			if(f.exists()) {
				return true;
			}
		}
		return false;
	}

	public static BufferedImage getIcon(boolean favIcon) {
		File[] iconPaths = Launch.getInstance().config.icon.get();
		if(iconPaths != null && hasIcon(iconPaths)) {
			for(File icon : iconPaths) {
				if(!icon.exists()) {
					continue;
				}
				try {
					return ImageIO.read(icon);
				} catch (IOException e) {
					System.err.println("Could not load icon at: " + icon.getAbsolutePath());
				}
			}
		}
		if(!favIcon) {
			try {
				return getIcon("/icon_32x32.png");
			} catch (IOException e) {
				System.err.println("Could not load the default icon");
			}
		}
		try {
			return getIcon("/favicon.png");
		} catch (IOException e) {
			System.err.println("Could not load favicon.png");
		}
		return null;
	}

	private static BufferedImage getIcon(String icon) throws IOException {
		return ImageIO.read(Inject.class.getResourceAsStream(icon));
	}

	private static ByteBuffer loadIcon(String icon) throws IOException {
		return loadIcon(getIcon(icon));
	}
}
