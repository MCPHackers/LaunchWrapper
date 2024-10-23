package org.mcphackers.launchwrapper.util;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import org.mcphackers.launchwrapper.Launch;
import org.mcphackers.launchwrapper.inject.Inject;

public final class IconUtils {

	private IconUtils() {
	}

	private static ByteBuffer loadIcon(BufferedImage icon) {
		final int[] rgb = icon.getRGB(0, 0, icon.getWidth(), icon.getHeight(), null, 0, icon.getWidth());

		final ByteBuffer buffer = ByteBuffer.allocate(4 * rgb.length);
		for (int color : rgb) {
			buffer.putInt(color << 8 | ((color >> 24) & 0xFF));
		}
		buffer.flip();
		return buffer;
	}

	private static BufferedImage getIcon(String icon) throws IOException {
		return ImageIO.read(Inject.class.getResourceAsStream(icon));
	}

	private static ByteBuffer loadIcon(String icon) throws IOException {
		return loadIcon(getIcon(icon));
	}

	public static ByteBuffer[] loadIcon() {
		return loadIcon(false);
	}

	public static ByteBuffer[] loadIcon(boolean favIcon) {
		File[] iconPaths = Launch.getInstance().config.icon.get();
		if (iconPaths == null || !hasIcon(iconPaths)) {
			try {
				if (!favIcon) {
					return new ByteBuffer[] {
						loadIcon("/icon_16x16.png"),
						loadIcon("/icon_32x32.png"),
						loadIcon("/icon_48x48.png"),
						loadIcon("/icon_256x256.png")
					};
				} else {
					return new ByteBuffer[] {
						loadIcon("/favicon.png")
					};
				}
			} catch (IOException e) {
				Launch.LOGGER.logErr("Could not load the default icon", e);
			}
			return null;
		}
		List<ByteBuffer> processedIcons = new ArrayList<ByteBuffer>();
		for (File icon : iconPaths) {
			if (!icon.exists()) {
				continue;
			}
			try {
				processedIcons.add(loadIcon(ImageIO.read(icon)));
			} catch (IOException e) {
				Launch.LOGGER.logErr("Could not load icon at: " + icon.getAbsolutePath(), e);
			}
		}
		return processedIcons.toArray(new ByteBuffer[processedIcons.size()]);
	}

	private static boolean hasIcon(File[] icons) {
		for (File f : icons) {
			if (f.exists()) {
				return true;
			}
		}
		return false;
	}

	public static BufferedImage getIcon(boolean favIcon) {
		File[] iconPaths = Launch.getInstance().config.icon.get();
		if (iconPaths != null && hasIcon(iconPaths)) {
			for (File icon : iconPaths) {
				if (!icon.exists()) {
					continue;
				}
				try {
					return ImageIO.read(icon);
				} catch (IOException e) {
					Launch.LOGGER.logErr("Could not load icon at: " + icon.getAbsolutePath(), e);
				}
			}
		}
		try {
			if (!favIcon) {
				return getIcon("/icon_256x256.png");
			} else {
				return getIcon("/favicon.png");
			}
		} catch (IOException e) {
			Launch.LOGGER.logErr("Could not load icon", e);
		}
		return null;
	}
}
