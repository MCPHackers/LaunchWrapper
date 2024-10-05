package org.mcphackers.launchwrapper.protocol;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

import org.mcphackers.launchwrapper.LaunchConfig;

public class LegacyURLStreamHandler extends URLStreamHandlerProxy {

	private LaunchConfig config;
	private AssetRequests assets;
	private SkinRequests skins;
	private File levelSaveDir;

	public LegacyURLStreamHandler(LaunchConfig config) {
		this.config = config;
		this.assets = new AssetRequests(config.assetsDir.get(), config.assetIndex.get());
		this.skins = new SkinRequests(config.gameDir.get(), config.skinOptions.get(), config.skinProxy.get());
		this.levelSaveDir = new File(config.gameDir.get(), "levels");
	}

	@Override
	protected URLConnection openConnection(URL url) throws IOException {
		String host = url.getHost();
		String path = url.getPath();
		String file = url.getFile();
		if(host.endsWith(".minecraft.net") || host.equals("s3.amazonaws.com")) {
			if(path.equals("/game/joinserver.jsp"))
				// TODO: update this to use the "sessionserver.mojang.com" API instead?
				return openDirectConnection(new URL("https", "session.minecraft.net", file));
			if(path.equals("/login/session.jsp") || host.equals("login.minecraft.net") && path.equals("/session")) {
				// UnlicensedCopyText injection does this instead. (It doesn't fire the check thread for some reason)
				if(config.unlicensedCopy.get()) {
					return new BasicResponseURLConnection(url, 400, "");
				} else {
					return new BasicResponseURLConnection(url, "ok");
				}
			}
			if(path.equals("/game/"))
				return new BasicResponseURLConnection(url, "42069");
			if(path.equals("/haspaid.jsp"))
				return new BasicResponseURLConnection(url, "true"); // TODO Where is this used?
			if(path.contains("/level/save.html"))
				return new SaveLevelURLConnection(url, levelSaveDir);
			if(path.contains("/level/load.html"))
				return new LoadLevelURLConnection(url, levelSaveDir);
			if(path.equals("/listmaps.jsp"))
				return new ListLevelsURLConnection(url, levelSaveDir);
			if(path.startsWith("/MinecraftResources/") || path.startsWith("/resources/"))
				return new AssetURLConnection(url, assets);
			if(path.startsWith("/MinecraftSkins/") || path.startsWith("/skin/") || path.startsWith("/MinecraftCloaks/") || path.startsWith("/cloak/"))
				return new SkinURLConnection(url, skins);
			if(host.equals("assets.minecraft.net") && path.equals("/1_6_has_been_released.flag"))
				if(config.oneSixFlag.get())
					return new BasicResponseURLConnection(url, "https://web.archive.org/web/20130702232237if_/https://mojang.com/2013/07/minecraft-the-horse-update/");
				else
					return new BasicResponseURLConnection(url, "");
			
			if(host.equals("snoop.minecraft.net")) {
				// Don't snoop
				if(path.equals("/client") || path.equals("/server"))
					return new BasicResponseURLConnection(url, "");
			}
			if(path.equals("/heartbeat.jsp"))
				return new HeartbeatURLConnection(url);
			// TODO redirect to something actually useful. Like a local self hosted realm
			if(host.equals("mcoapi.minecraft.net")) {
				if(path.equals("/mco/available"))
					return new BasicResponseURLConnection(url, "true");
				if(path.equals("/mco/client/outdated"))
					return new BasicResponseURLConnection(url, "false");
				// if(path.equals("/payments/unused"))
				// 	return new BasicResponseURLConnection(url, "0");
				// if(path.equals("/invites/count/pending"))
				// 	return new BasicResponseURLConnection(url, "0");
				// if(path.equals("/invites/pending"))
				// 	return new BasicResponseURLConnection(url, "{}");
				// if(path.equals("/worlds"))
				// 	return new BasicResponseURLConnection(url, "");
				// if(path.equals("/worlds/templates"))
				// 	return new BasicResponseURLConnection(url, "");
				// if(path.equals("/worlds/test/$LOCATION_ID"))
				// 	return new BasicResponseURLConnection(url, "1");
			}
			//FIXME server sends their own skin as packet
			if(host.equals("textures.minecraft.net")) {
				if(path.startsWith("/texture/")) {
					return new TextureURLConnection(url, skins);
				}
			}
		}
		if(host.equals("sessionserver.mojang.com")) {
			if(path.startsWith("/session/minecraft/profile/")) {
				return new ProfileURLConnection(url, skins);
			}
		}
		return openDirectConnection(url);
	}
}
