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

public class Inject {
	private static final BufferedImage DEFAULT_ICON = getIcon();

	public static AppletWrapper getApplet() {
		return new AppletWrapper(Launch.getConfig().getArgsAsMap());
	}

	/**
	 * Indev load level injection
	 */
	public static File getLevelFile(int index) {
		return new File(Launch.getConfig().gameDir.get(), "levels/level" + index + ".dat");
	}

	/**
	 * Indev save level injection
	 */
	public static File saveLevel(int index, String levelName) {
		final int maxLevels = 5;
		File levels = new File(Launch.getConfig().gameDir.get(), "levels");
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

	public static ByteBuffer loadIcon(BufferedImage icon) {
		final int[] rgb = icon.getRGB(0, 0, icon.getWidth(), icon.getHeight(), null, 0, icon.getWidth());

		final ByteBuffer buffer = ByteBuffer.allocate(4 * rgb.length);
		for(int color : rgb) {
			buffer.putInt(color << 8 | ((color >> 24) & 0xFF));
		}
		buffer.flip();
		return buffer;
	}

	public static ByteBuffer[] loadDefaultIcon(boolean grassBlock) {
		if(!grassBlock) {
			try {
				return new ByteBuffer[] { loadIcon("/icon_16x16.png"), loadIcon("/icon_32x32.png") };
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return new ByteBuffer[] { loadIcon(DEFAULT_ICON) };
	}

	public static ByteBuffer[] loadIcons() {
		List<ByteBuffer> processedIcons = new ArrayList<ByteBuffer>();
		for(File icon : Launch.getConfig().icon.get()) {
			if(!icon.exists()) {
				continue;
			}
			try {
				processedIcons.add(loadIcon(ImageIO.read(icon)));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return processedIcons.toArray(new ByteBuffer[0]);
	}

	public static ByteBuffer[] loadIcon(File icon) {
		try {
			return new ByteBuffer[] { loadIcon(ImageIO.read(icon)) };
		} catch (IOException e) {
			return null;
		}
	}

	private static ByteBuffer loadIcon(String icon) throws IOException {
		return loadIcon(ImageIO.read(Inject.class.getResourceAsStream(icon)));
	}

	public static BufferedImage getIcon() {
		if(DEFAULT_ICON != null) {
			return DEFAULT_ICON;
		}
		try {
			return ImageIO.read(Inject.class.getResourceAsStream("/favicon.png"));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
}
