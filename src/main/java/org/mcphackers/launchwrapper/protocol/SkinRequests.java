package org.mcphackers.launchwrapper.protocol;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;

import org.json.JSONObject;
import org.mcphackers.launchwrapper.util.Base64;
import org.mcphackers.launchwrapper.util.ImageUtils;
import org.mcphackers.launchwrapper.util.Util;

public class SkinRequests {
	// name->uuid requests have a rate limit, so here's cache
	private static HashMap<String, String> nameToUUID = new HashMap<String, String>();

	// No skins and capes for you. Sorry guys
	private static final String[] BLACKLISTED_NAMES = {"Player", "DemoUser"}; 

	public static String getUUIDfromName(String username) {
		for(String s : BLACKLISTED_NAMES) {
			if(s.equals(username)) {
				return null;
			}
		}
		String cachedUUID = nameToUUID.get(username);

		if(cachedUUID != null) {
			return cachedUUID;
		} else {
			JSONObject obj = requestUUIDfromName(username);
			if(obj != null) {
				String uuid = obj.optString("id");

				nameToUUID.put(username, uuid);
				return uuid;
			}
		}

		return null;
	}

	public static JSONObject requestUUIDfromName(String name) {
		try {
			URL profileURL = new URL("https://api.mojang.com/users/profiles/minecraft/" + name);
			HttpURLConnection connection = (HttpURLConnection)profileURL.openConnection();
			if(connection.getResponseCode() == 404) {
				return null;
			}
			InputStream connStream = connection.getInputStream();
			JSONObject uuidJson = new JSONObject(new String(Util.readStream(connStream), "UTF-8"));

			return uuidJson;
		} catch (Throwable t) {
			t.printStackTrace();
			return null;
		}
	}

	public static SkinData fetchSkin(String uuid) {
		try {
			URL uuidtoprofileURL = new URL("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid);

			JSONObject profilejson = new JSONObject(new String(Util.readStream(uuidtoprofileURL.openStream()), "UTF-8"));
			String base64tex = profilejson.getJSONArray("properties").getJSONObject(0).optString("value");
			String texjsonstr = new String(Base64.decode(base64tex), "UTF-8");

			JSONObject texjson = new JSONObject(texjsonstr);
			JSONObject txts = texjson.optJSONObject("textures");
			JSONObject skinjson = txts.optJSONObject("SKIN");
			String skinURLstr = skinjson.optString("url");

			byte[] officialCape = null;
			byte[] officialSkin = null;

			JSONObject capejson = txts.optJSONObject("CAPE");
			if(capejson != null) {
				String capeURLstr = capejson.optString("url");
				URL capeURL = new URL(capeURLstr);

				officialCape = Util.readStream(capeURL.openStream());
			}

			boolean slim = false;
			JSONObject metadata = skinjson.optJSONObject("metadata");
			if(metadata != null) {
				slim = "slim".equals(metadata.optString("model"));
			}

			URL skinURL = new URL(skinURLstr);
			officialSkin = Util.readStream(skinURL.openStream());

			return new SkinData(officialSkin, officialCape, slim);
		} catch (Throwable t) {
			t.printStackTrace();
		}
		return null;
	}

	public static class SkinData {
		public byte[] skin;
		public byte[] cape;
		public boolean slim;

		public SkinData(byte[] skin, byte[] cape, boolean slim) {
			this.skin = skin;
			this.cape = cape;
			this.slim = slim;
		}
	}

	private SkinType skinType;
	private List<SkinOption> skinOptions;
	private File localSkinDirectory;

	public SkinRequests(File dir, List<SkinOption> options, SkinType type) {
		localSkinDirectory = dir;
		skinOptions = options;
		skinType = type;
	}

	public SkinData getLocalSkin(String name) {
		byte[] skin = null;
		byte[] cape = null;
		boolean slim = false;
		
		File skinFile = new File(localSkinDirectory, name + ".png");
		if(skinFile.exists()) {
			try {
				skin = Util.readStream(new FileInputStream(skinFile));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		skinFile = new File(localSkinDirectory, name + ".slim.png");
		if(skinFile.exists() && skin == null) {
			try {
				skin = Util.readStream(new FileInputStream(skinFile));
				slim = true;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		File capeFile = new File(localSkinDirectory, "cape/" + name + ".png");
		if(capeFile.exists()) {
			try {
				cape = Util.readStream(new FileInputStream(capeFile));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return new SkinData(skin, cape, slim);
	}

	public byte[] getCape(String name) {
		SkinData skindata = getLocalSkin(name);
		if(skindata.cape == null) {
			String uuid = getUUIDfromName(name);
			if(uuid == null) {
				return null;
			}
			skindata = fetchSkin(uuid);
		}
		if(skindata == null) {
			return null;
		}

		byte[] cape = null;
		try {
			if(skindata.cape != null) {
				switch(skinType) {
					case PRE_19A:
					case CLASSIC:
					case PRE_B1_9:
					case PRE_1_8:
					case PRE_1_9:
						ByteArrayInputStream bis = new ByteArrayInputStream(skindata.cape);

						ImageUtils imgu = new ImageUtils(bis);
						imgu = imgu.crop(0, 0, 64, 32);

						cape = imgu.getInByteForm();
						break;
					case DEFAULT:
						return skindata.cape;
				}
			}
		} catch (Throwable t) {
			t.printStackTrace();
		}
		return cape;
	}

	public byte[] getSkin(String name) {
		SkinData skindata = getLocalSkin(name);
		if(skindata.skin == null) {
			String uuid = getUUIDfromName(name);
			if(uuid == null) {
				return null;
			}
			skindata = fetchSkin(uuid);
		}
		if(skindata == null) {
			return null;
		}

		byte[] skin = null;
		try {
			if(skindata.skin != null) {
				ByteArrayInputStream bis = new ByteArrayInputStream(skindata.skin);
				ImageUtils imgu = new ImageUtils(bis);
				switch(skinType) {
					case PRE_19A:
					case CLASSIC:
					case PRE_B1_9:
					case PRE_1_8:
					case PRE_1_9:
						if(skinOptions.contains(SkinOption.REMOVE_HAT))
							imgu.clearArea(32, 0, 32, 16);
						if(!skinOptions.contains(SkinOption.IGNORE_LAYERS))
							fixLayerAlpha(imgu);
						break;
					case DEFAULT:
						if(skinOptions.contains(SkinOption.REMOVE_HAT))
							imgu.clearArea(32, 0, 32, 16);
						break;
				}

				switch(skinType) {
					case PRE_19A:
					case CLASSIC:
						if(!skinOptions.contains(SkinOption.IGNORE_LAYERS))
							overlayHeadLayer(imgu);
					case PRE_B1_9:
					case PRE_1_8:
						if(skinOptions.contains(SkinOption.USE_LEFT_ARM))
							useLeftArm(imgu, true);
						if(skinOptions.contains(SkinOption.USE_LEFT_LEG))
							useLeftLeg(imgu, true);
						if(imgu.getImage().getHeight() == 64 && !skinOptions.contains(SkinOption.IGNORE_LAYERS))
							overlayBodyLayers(imgu);
						if(skindata.slim && !skinOptions.contains(SkinOption.IGNORE_ALEX))
							alexToSteve(imgu);
						if(skinType == SkinType.PRE_B1_9 || skinType == SkinType.CLASSIC || skinType == SkinType.PRE_19A)
							rotateBottomTX(imgu);
						if(skinType == SkinType.PRE_19A)
							flipSkin(imgu);
						imgu = imgu.crop(0, 0, 64, 32);
					case PRE_1_9:
					case DEFAULT:
						break;
				}
				skin = imgu.getInByteForm();
				FileOutputStream fos = new FileOutputStream(new File("/home/lassebq/skin.png"));
				fos.write(skin);
				fos.close();
			}
		} catch (Throwable t) {
			t.printStackTrace();
		}
		return skin;
	}

	private static void fixLayerAlpha(ImageUtils imgu) {
		BufferedImage img = imgu.getImage();
		// Head
		for(int i = 0; i < 32; i++) {
			for(int j = 0; j < 16; j++) {
				int color1 = img.getRGB(i, j);
				int color2 = img.getRGB(i+32, j);
				int alpha = color2 >> 24;
				if(alpha != -1 && alpha != 0x00) {
					img.setRGB(i, j, ImageUtils.mergeColor(color1, color2));
					img.setRGB(i+32, j, 0);
				}
			}
		}
		if(img.getHeight() <= 32) {
			return;
		}
		// Body
		for(int i = 0; i < 56; i++) {
			for(int j = 16; j < 32; j++) {
				int color1 = img.getRGB(i, j);
				int color2 = img.getRGB(i, j+16);
				int alpha = color2 >> 24;
				if(alpha != -1 && alpha != 0x00) {
					img.setRGB(i, j, ImageUtils.mergeColor(color1, color2));
					img.setRGB(i, j+16, 0);
				}
			}
		}
		// Left leg
		for(int i = 0; i < 16; i++) {
			for(int j = 48; j < 64; j++) {
				int color1 = img.getRGB(i+16, j);
				int color2 = img.getRGB(i, j);
				int alpha = color2 >> 24;
				if(alpha != -1 && alpha != 0x00) {
					img.setRGB(i+16, j, ImageUtils.mergeColor(color1, color2));
					img.setRGB(i, j, 0);
				}
			}
		}
		// Left arm
		for(int i = 0; i < 16; i++) {
			for(int j = 48; j < 64; j++) {
				int color1 = img.getRGB(i+16, j);
				int color2 = img.getRGB(i, j);
				int alpha = color2 >> 24;
				if(alpha != -1 && alpha != 0x00) {
					img.setRGB(i+16, j, ImageUtils.mergeColor(color1, color2));
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
