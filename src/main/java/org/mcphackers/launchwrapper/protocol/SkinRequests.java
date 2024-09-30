package org.mcphackers.launchwrapper.protocol;

import static org.mcphackers.launchwrapper.protocol.URLStreamHandlerProxy.openDirectConnection;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.mcphackers.launchwrapper.util.Base64;
import org.mcphackers.launchwrapper.util.ImageUtils;
import org.mcphackers.launchwrapper.util.Util;

public class SkinRequests {
	// name->uuid requests have a rate limit, so here's cache
	private static HashMap<String, String> nameToUUID = new HashMap<String, String>();
	// texture hash->owner profile for skin textures
	private static HashMap<String, Profile> profileMap = new HashMap<String, Profile>();

	// No skins and capes for you. Sorry guys
	private static final String[] BLACKLISTED_NAMES = {"Player", "DemoUser"}; 

	private SkinType skinType;
	private List<SkinOption> skinOptions;
	private File localSkinDirectory;
	private File assetsSkinCache;

	public SkinRequests(File dir, List<SkinOption> options, SkinType type) {
		this.localSkinDirectory = new File(dir, "skin");
		this.assetsSkinCache = new File(dir, "assets/skins");
		this.skinOptions = options;
		this.skinType = type;
	}

	public static class Skin {
		public byte[] data;
		public boolean slim;

		public Skin(byte[] data, boolean slim) {
			this.data = data;
			this.slim = slim;
		}
	}

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
			HttpURLConnection connection = (HttpURLConnection)openDirectConnection(profileURL);
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

	public Profile getProfileFromTexture(String textureHash) {
		return profileMap.get(textureHash);
	}

	public class Profile {
		JSONObject json;
		public final String name;
		public final String uuid;
		public String skinTex;
		public String capeTex;
		private boolean slim;

		public Profile(JSONObject json, String name, String uuid, String skinTex, String capeTex, boolean slim) {
			this.json = json;
			this.name = name;
			this.uuid = uuid;
			this.skinTex = skinTex;
			this.capeTex = capeTex;
			this.slim = slim;
		}

		public Skin getSkin() {
			if(skinTex == null) {
				return null;
			}
			try {
				File localSkin = new File(assetsSkinCache, skinTex.substring(0, 2) + "/" + skinTex);
				if(localSkin.exists()) {
					return new Skin(Util.readStream(new FileInputStream(localSkin)), slim);
				}
				URL skinURL = new URL("http://textures.minecraft.net/texture/" + skinTex);
				return new Skin(Util.readStream(openDirectConnection(skinURL).getInputStream()), slim);
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			}
		}

		public byte[] getCape() {
			if(capeTex == null) {
				return null;
			}
			try {
				File localSkin = new File(assetsSkinCache, capeTex.substring(0, 2) + "/" + capeTex);
				if(localSkin.exists()) {
					return Util.readStream(new FileInputStream(localSkin));
				}
				URL capeURL = new URL("http://textures.minecraft.net/texture/" + capeTex);
				return Util.readStream(openDirectConnection(capeURL).getInputStream());
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			}
		}
	}

	public Profile getProfile(String uuid) {
		try {
			URL uuidtoprofileURL = new URL("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid);
			JSONObject profileJson = new JSONObject(new String(Util.readStream(openDirectConnection(uuidtoprofileURL).getInputStream()), "UTF-8"));
			JSONArray properties = profileJson.optJSONArray("properties");
			if(properties == null) {
				return null;
			}
			String texjsonstr = null;
			for(int i = 0; i < properties.length(); i++) {
				if(!"textures".equals(properties.getJSONObject(i).optString("name"))) {
					continue;
				}
				String base64tex = properties.getJSONObject(i).optString("value");
				texjsonstr = new String(Base64.decode(base64tex), "UTF-8");
			}
			String name = profileJson.optString("name", null);
			if(texjsonstr == null) {
				return new Profile(profileJson, name, uuid, null, null, false);
			}
			boolean slim = false;
			String skinHash = null;
			String capeHash = null;

			JSONObject texjson = new JSONObject(texjsonstr);
			JSONObject txts = texjson.optJSONObject("textures");
			if(txts != null) {
				JSONObject skinjson = txts.optJSONObject("SKIN");
				if(skinjson != null) {
					JSONObject metadata = skinjson.optJSONObject("metadata");
					if(metadata != null) {
						slim = "slim".equals(metadata.optString("model"));
					}
					String skinURLstr = skinjson.optString("url");
					skinHash = skinURLstr.substring(skinURLstr.lastIndexOf('/') + 1);
				}
				skinjson = txts.optJSONObject("CAPE");
				if(skinjson != null) {
					String skinURLstr = skinjson.optString("url");
					capeHash = skinURLstr.substring(skinURLstr.lastIndexOf('/') + 1);
				}
			}
			Profile p = new Profile(profileJson, name, uuid, skinHash, capeHash, slim);
			profileMap.put(skinHash, p);
			profileMap.put(capeHash, p);
			appendLocalSkins(p);
			return p;

		} catch (Throwable t) {
			t.printStackTrace();
			return null;
		}
	}

	private JSONObject getTexturesObj(JSONObject profile) {
		JSONArray properties = profile.optJSONArray("properties");
		if(properties == null) {
			return null;
		}
		for(int i = 0; i < properties.length(); i++) {
			if(!"textures".equals(properties.getJSONObject(i).optString("name"))) {
				continue;
			}
			return properties.getJSONObject(i);
		}
		return null;
	}

	private void appendLocalSkins(Profile p) throws IOException {
		Skin skin = getLocalSkin(p.name);
		byte[] cape = getLocalCape(p.name);
		if(skin != null || cape != null) {
			JSONObject obj = getTexturesObj(p.json);
			String base64Str = obj.optString("value", null);
			if(base64Str == null) {
				return;
			}
			JSONObject texjson = new JSONObject(new String(Base64.decode(base64Str), "UTF-8"));
			JSONObject txts = texjson.optJSONObject("textures");
			if(txts != null) {
				JSONObject skinjson = txts.optJSONObject("SKIN");
				if(skin != null && skinjson != null) {
					JSONObject metadata = skinjson.optJSONObject("metadata");
					if(skin.slim) {
						if(metadata != null) {
							metadata.put("model", "slim");
						} else {
							metadata = new JSONObject();
							skinjson.put("metadata", metadata);
							metadata.put("model", "slim");
						}
					} else if(metadata != null) {
						metadata.remove("model");
					}
				}
			}
			texjson.put("timestamp", System.currentTimeMillis() / 1000L);
			if(skin != null) {
				JSONObject skinjson = txts.optJSONObject("SKIN");
				if(skinjson == null) {
					skinjson = new JSONObject();
					txts.put("SKIN", skinjson);
				}
				String sha256 = Util.getSHA256(new ByteArrayInputStream(skin.data));
				skinjson.put("url", "http://textures.minecraft.net/texture/" + sha256);
				profileMap.put(sha256, p);
				p.skinTex = sha256;
			}
			if(cape != null) {
				JSONObject capejson = txts.optJSONObject("CAPE");
				if(capejson == null) {
					capejson = new JSONObject();
					txts.put("CAPE", capejson);
				}
				String sha256 = Util.getSHA256(new ByteArrayInputStream(cape));
				capejson.put("url", "http://textures.minecraft.net/texture/" + sha256);
				profileMap.put(sha256, p);
				p.capeTex = sha256;
			}
			obj.put("value", Base64.encodeBytes(texjson.toString().getBytes()));
		}
	}

	public byte[] getLocalCape(String name) {
		File capeFile = new File(localSkinDirectory, "cape/" + name + ".png");
		if(capeFile.exists()) {
			try {
				return Util.readStream(new FileInputStream(capeFile));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	public Skin getLocalSkin(String name) {
		
		File skinFile = new File(localSkinDirectory, name + ".png");
		if(skinFile.exists()) {
			try {
				return new Skin(Util.readStream(new FileInputStream(skinFile)), false);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		skinFile = new File(localSkinDirectory, name + ".slim.png");
		if(skinFile.exists()) {
			try {
				return new Skin(Util.readStream(new FileInputStream(skinFile)), true);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	public byte[] getCape(String name) {
		byte[] cape = getLocalCape(name);
		if(cape != null) {
			return cape;
		}
		String uuid = getUUIDfromName(name);
		if(uuid == null) {
			return null;
		}
		return getCape(getProfile(uuid));
	}

	public byte[] getCape(Profile p) {
		if(p == null) {
			return null;
		}
		byte[] cape = getLocalCape(p.name);
		if(cape != null) {
			return cape;
		}
		return convertCape(p.getCape(), skinType);
	}

	public byte[] getSkin(String name) {
		Skin skinData = getLocalSkin(name);
		if(skinData != null) {
			return skinData.data;
		}
		String uuid = getUUIDfromName(name);
		if(uuid == null) {
			return null;
		}
		return getSkin(getProfile(uuid));
	}

	public byte[] getSkin(Profile p) {
		if(p == null) {
			return null;
		}
		Skin skin = getLocalSkin(p.name);
		if(skin != null) {
			return skin.data;
		}
		skin = p.getSkin();
		if(skin == null) {
			return null;
		}
		return convertSkin(skin.data, p.slim, skinType, skinOptions);
	}

	public static byte[] convertCape(byte[] cape, SkinType skinType) {
		// Capes are always 64x32
		return cape;
	}

	public static byte[] convertSkin(byte[] skin, boolean slim, SkinType skinType, List<SkinOption> skinOptions) {
		if(skin == null) {
			return skin;
		}
		try {
			ByteArrayInputStream bis = new ByteArrayInputStream(skin);
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
					if(slim && !skinOptions.contains(SkinOption.IGNORE_ALEX))
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
		} catch (Exception e) {
			e.printStackTrace();
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
