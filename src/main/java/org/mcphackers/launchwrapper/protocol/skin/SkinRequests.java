package org.mcphackers.launchwrapper.protocol.skin;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.mcphackers.launchwrapper.util.ImageUtils;

public class SkinRequests {

	private SkinType skinType;
	private List<SkinOption> skinOptions;
	private File assetsDir;
	private List<SkinProvider> providers = new ArrayList<SkinProvider>();

	public SkinRequests(File gameDir, File assetsDir, List<SkinOption> options, SkinType type) {
		this.assetsDir = assetsDir;
		this.skinOptions = options;
		this.skinType = type;
		providers.add(new LocalSkinProvider(new File(gameDir, "skins")));
		providers.add(new MinecraftCapesProvider());
		providers.add(new SkinMCCapeProvider());
		providers.add(new MojangSkinProvider());
		providers.add(new ElyBySkinProvider());
	}

	public Skin getSkin(String id, String username, SkinTexture type) {
		for (SkinProvider provider : providers) {
			Skin skin = provider.getSkin(id, username, type);
			if (skin == null) {
				continue;
			}
			String hash = skin.getSHA256();
			if (hash != null) {
				File f = new File(assetsDir, "skins/" + hash.substring(0, 2) + "/" + hash);
				if (f.isFile() && f.canRead()) {
					return new LocalSkin(f, skin.isSlim());
				}
			}
			return skin;
		}
		return null;
	}

	public byte[] getConvertedSkin(String id, String username, SkinTexture type) {
		for (SkinProvider provider : providers) {
			Skin skin = provider.getSkin(id, username, type);
			if (skin == null) {
				continue;
			}
			String hash = skin.getSHA256();
			if (hash != null) {
				File f = new File(assetsDir, "skins/" + hash.substring(0, 2) + "/" + hash);
				if (f.isFile() && f.canRead()) {
					try {
						LocalSkin localSkin = new LocalSkin(f, skin.isSlim());
						return type == SkinTexture.SKIN ? convertSkin(localSkin) : convertCape(localSkin);
					} catch (IOException e) {
						// ignore
					}
				}
			}
			try {
				byte[] skinData = type == SkinTexture.SKIN ? convertSkin(skin) : convertCape(skin);
				if (skinData == null) {
					continue;
				}
				return skinData;
			} catch (FileNotFoundException e) {
				continue;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	private byte[] convertCape(Skin skin) throws IOException {
		// Capes are always 64x32
		return skin.getData();
	}

	private byte[] convertSkin(Skin skin) throws IOException {
		byte[] data = skin.getData();
		if (data == null) {
			return null;
		}
		ByteArrayInputStream bis = new ByteArrayInputStream(data);
		ImageUtils imgu = new ImageUtils(bis);
		switch (skinType) {
			case PRE_19A:
			case CLASSIC:
			case PRE_B1_9:
			case PRE_1_8:
			case PRE_1_9:
				if (skinOptions.contains(SkinOption.REMOVE_HAT))
					imgu.clearArea(32, 0, 32, 16);
				if (!skinOptions.contains(SkinOption.IGNORE_LAYERS))
					fixLayerAlpha(imgu);
				break;
			case DEFAULT:
				if (skinOptions.contains(SkinOption.REMOVE_HAT))
					imgu.clearArea(32, 0, 32, 16);
				break;
		}

		switch (skinType) {
			case PRE_19A:
			case CLASSIC:
				if (!skinOptions.contains(SkinOption.IGNORE_LAYERS))
					overlayHeadLayer(imgu);
			case PRE_B1_9:
			case PRE_1_8:
				if (skinOptions.contains(SkinOption.USE_LEFT_ARM))
					useLeftArm(imgu, true);
				if (skinOptions.contains(SkinOption.USE_LEFT_LEG))
					useLeftLeg(imgu, true);
				if (imgu.getImage().getHeight() == 64 && !skinOptions.contains(SkinOption.IGNORE_LAYERS))
					overlayBodyLayers(imgu);
				if (skin.isSlim() && !skinOptions.contains(SkinOption.IGNORE_ALEX))
					alexToSteve(imgu);
				if (skinType == SkinType.PRE_B1_9 || skinType == SkinType.CLASSIC || skinType == SkinType.PRE_19A)
					rotateBottomTX(imgu);
				if (skinType == SkinType.PRE_19A)
					flipSkin(imgu);
				imgu = imgu.crop(0, 0, 64, 32);
			case PRE_1_9:
			case DEFAULT:
				break;
		}
		return imgu.getInByteForm();
	}

	private static void fixLayerAlpha(ImageUtils imgu) {
		BufferedImage img = imgu.getImage();
		// Head
		for (int i = 0; i < 32; i++) {
			for (int j = 0; j < 16; j++) {
				int color1 = img.getRGB(i, j);
				int color2 = img.getRGB(i + 32, j);
				int alpha = color2 >> 24;
				if (alpha != -1 && alpha != 0x00) {
					img.setRGB(i, j, ImageUtils.mergeColor(color1, color2));
					img.setRGB(i + 32, j, 0);
				}
			}
		}
		if (img.getHeight() <= 32) {
			return;
		}
		// Body
		for (int i = 0; i < 56; i++) {
			for (int j = 16; j < 32; j++) {
				int color1 = img.getRGB(i, j);
				int color2 = img.getRGB(i, j + 16);
				int alpha = color2 >> 24;
				if (alpha != -1 && alpha != 0x00) {
					img.setRGB(i, j, ImageUtils.mergeColor(color1, color2));
					img.setRGB(i, j + 16, 0);
				}
			}
		}
		// Left leg
		for (int i = 0; i < 16; i++) {
			for (int j = 48; j < 64; j++) {
				int color1 = img.getRGB(i + 16, j);
				int color2 = img.getRGB(i, j);
				int alpha = color2 >> 24;
				if (alpha != -1 && alpha != 0x00) {
					img.setRGB(i + 16, j, ImageUtils.mergeColor(color1, color2));
					img.setRGB(i, j, 0);
				}
			}
		}
		// Left arm
		for (int i = 0; i < 16; i++) {
			for (int j = 48; j < 64; j++) {
				int color1 = img.getRGB(i + 16, j);
				int color2 = img.getRGB(i, j);
				int alpha = color2 >> 24;
				if (alpha != -1 && alpha != 0x00) {
					img.setRGB(i + 16, j, ImageUtils.mergeColor(color1, color2));
					img.setRGB(i, j, 0);
				}
			}
		}
	}

	private static void flipSkin(ImageUtils imgu) {
		BufferedImage sideLeft;
		BufferedImage sideRight;
		// head
		imgu.setArea(8, 0, imgu.crop(8, 0, 8, 16).flip(true, false).getImage());
		imgu.setArea(16, 0, imgu.crop(16, 0, 8, 8).flip(true, false).getImage());
		imgu.setArea(24, 8, imgu.crop(24, 8, 8, 8).flip(true, false).getImage());
		sideLeft = imgu.crop(0, 8, 8, 8).flip(true, false).getImage();
		sideRight = imgu.crop(16, 8, 8, 8).flip(true, false).getImage();
		imgu.setArea(16, 8, sideLeft);
		imgu.setArea(0, 8, sideRight);
		// body
		imgu.setArea(20, 16, imgu.crop(20, 16, 8, 16).flip(true, false).getImage());
		imgu.setArea(28, 16, imgu.crop(28, 16, 8, 4).flip(true, false).getImage());
		imgu.setArea(32, 20, imgu.crop(32, 20, 8, 12).flip(true, false).getImage());
		sideLeft = imgu.crop(16, 20, 4, 12).flip(true, false).getImage();
		sideRight = imgu.crop(28, 20, 4, 12).flip(true, false).getImage();
		imgu.setArea(28, 20, sideLeft);
		imgu.setArea(16, 20, sideRight);
		// leg
		imgu.setArea(4, 16, imgu.crop(4, 16, 4, 16).flip(true, false).getImage());
		imgu.setArea(8, 16, imgu.crop(8, 16, 4, 4).flip(true, false).getImage());
		imgu.setArea(12, 20, imgu.crop(12, 20, 4, 12).flip(true, false).getImage());
		sideLeft = imgu.crop(0, 20, 4, 12).flip(true, false).getImage();
		sideRight = imgu.crop(8, 20, 4, 12).flip(true, false).getImage();
		imgu.setArea(8, 20, sideLeft);
		imgu.setArea(0, 20, sideRight);
		// arm
		imgu.setArea(44, 16, imgu.crop(44, 16, 4, 16).flip(true, false).getImage());
		imgu.setArea(48, 16, imgu.crop(48, 16, 4, 4).flip(true, false).getImage());
		imgu.setArea(52, 20, imgu.crop(52, 20, 4, 12).flip(true, false).getImage());
		sideLeft = imgu.crop(40, 20, 4, 12).flip(true, false).getImage();
		sideRight = imgu.crop(48, 20, 4, 12).flip(true, false).getImage();
		imgu.setArea(48, 20, sideLeft);
		imgu.setArea(40, 20, sideRight);
	}

	public static void rotateBottomTX(ImageUtils imgu) {
		// bottom head
		imgu.setArea(16, 0, imgu.crop(16, 0, 8, 8).flip(false, true).getImage());
		// bottom head layer
		imgu.setArea(48, 0, imgu.crop(48, 0, 8, 8).flip(false, true).getImage());

		// bottom feet
		imgu.setArea(8, 16, imgu.crop(8, 16, 4, 4).flip(false, true).getImage());
		// bottom hand
		imgu.setArea(48, 16, imgu.crop(48, 16, 4, 4).flip(false, true).getImage());
		// bottom torso
		imgu.setArea(28, 16, imgu.crop(28, 16, 8, 4).flip(false, true).getImage());
	}

	public static void overlayBodyLayers(ImageUtils imgu) {
		// 32-64 body
		imgu.setArea(0, 16, imgu.crop(0, 32, 56, 16).getImage(), false);
		imgu.clearArea(0, 32, 56, 16);
	}

	public static void overlayHeadLayer(ImageUtils imgu) {
		imgu.setArea(0, 0, imgu.crop(32, 0, 32, 16).getImage(), false);
		imgu.clearArea(32, 0, 32, 16);
	}

	public static void useLeftLeg(ImageUtils imgu, boolean flipped) {
		// leg layer
		imgu.setArea(0, 32, imgu.crop(0, 48, 16, 16).flip(false, false).getImage());
		// leg

		imgu.setArea(0, 16, imgu.crop(16, 48, 16, 16).flip(false, false).getImage());
		imgu.setArea(4, 16, imgu.crop(4, 16, 4, 16).flip(true, false).getImage());
		imgu.setArea(8, 16, imgu.crop(8, 16, 4, 4).flip(true, false).getImage());
		imgu.setArea(12, 20, imgu.crop(12, 20, 4, 12).flip(true, false).getImage());
		BufferedImage sideLeft = imgu.crop(0, 20, 4, 12).flip(true, false).getImage();
		BufferedImage sideRight = imgu.crop(8, 20, 4, 12).flip(true, false).getImage();
		imgu.setArea(8, 20, sideLeft);
		imgu.setArea(0, 20, sideRight);
	}

	public static void useLeftArm(ImageUtils imgu, boolean flipped) {
		// arm layer
		imgu.setArea(40, 16, imgu.crop(32, 48, 16, 16).flip(false, false).getImage());
		// arm

		imgu.setArea(40, 32, imgu.crop(48, 48, 16, 16).flip(false, false).getImage());
		imgu.setArea(44, 16, imgu.crop(44, 16, 4, 16).flip(true, false).getImage());
		imgu.setArea(48, 16, imgu.crop(48, 16, 4, 4).flip(true, false).getImage());
		imgu.setArea(52, 20, imgu.crop(52, 20, 4, 12).flip(true, false).getImage());
		BufferedImage sideLeft = imgu.crop(40, 20, 4, 12).flip(true, false).getImage();
		BufferedImage sideRight = imgu.crop(48, 20, 4, 12).flip(true, false).getImage();
		imgu.setArea(48, 20, sideLeft);
		imgu.setArea(40, 20, sideRight);
	}

	public static void alexToSteve(ImageUtils imgu) {
		imgu.setArea(46, 16, imgu.crop(45, 16, 9, 16).getImage());
		imgu.setArea(50, 16, imgu.crop(49, 16, 2, 4).getImage());
		imgu.setArea(54, 20, imgu.crop(53, 20, 2, 12).getImage());
	}
}
