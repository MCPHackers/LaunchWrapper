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

	public LegacyURLStreamHandler(LaunchConfig config) {
		this.config = config;
		this.assets = new AssetRequests(config.assetsDir.get(), config.assetIndex.get());
		this.skins = new SkinRequests(new File(config.gameDir.get(), "skin"), config.skinOptions.get(), config.skinProxy.get());
	}

	@Override
	protected URLConnection openConnection(URL url) throws IOException {
		String host = url.getHost();
		String path = url.getPath();
		String file = url.getFile();
		if(host.endsWith(".minecraft.net") || host.equals("s3.amazonaws.com")) {
			if(path.equals("/game/joinserver.jsp"))
				// TODO: update this to use the "sessionserver.mojang.com" API instead?
				return super.openConnection(new URL("https", "session.minecraft.net", file));
			if(path.equals("/login/session.jsp") || host.equals("login.minecraft.net") && path.equals("/session")) {
				// UnlicensedCopyText injection does this instead. (It doesn't fire the check thread fro some reason)
				if(config.haspaid.get()) {
					return new BasicResponseURLConnection(url, "ok");
				} else {
					return new BasicResponseURLConnection(url, 400, "");
				}
			}
			if(path.equals("/game/"))
				return new BasicResponseURLConnection(url, "42069");
			if(path.equals("/client"))
				return new BasicResponseURLConnection(url, "idk"); // TODO figure out what this API endpoint is for
			if(path.equals("/haspaid.jsp"))
				return new BasicResponseURLConnection(url, "true"); // TODO Where is this used?
			if(path.contains("/level/save.html"))
				return new SaveLevelURLConnection(url, config.gameDir.get());
			if(path.contains("/level/load.html"))
				return new LoadLevelURLConnection(url, config.gameDir.get());
			if(path.equals("/listmaps.jsp"))
				return new ListLevelsURLConnection(url, config.gameDir.get());
			if(path.startsWith("/MinecraftResources/") || path.startsWith("/resources/"))
				return new AssetURLConnection(url, assets);
			if(path.startsWith("/MinecraftSkins/") || path.startsWith("/skin/") || path.startsWith("/MinecraftCloaks/") || path.startsWith("/cloak/"))
				return new SkinURLConnection(url, skins);
			if(host.equals("assets.minecraft.net") && path.equals("/1_6_has_been_released.flag"))
				if(config.oneSixFlag.get())
					return new BasicResponseURLConnection(url, "https://web.archive.org/web/20130702232237if_/https://mojang.com/2013/07/minecraft-the-horse-update/");
				else
					return new BasicResponseURLConnection(url, "");
			
			// TODO redirect to something actually useful. Like a local self hosted realm
			if(host.equals("mcoapi.minecraft.net")) {
				if(path.equals("/mco/available"))
					return new BasicResponseURLConnection(url, "true");
				if(path.equals("/mco/client/outdated"))
					return new BasicResponseURLConnection(url, "false");
				if(path.equals("/payments/unused"))
					return new BasicResponseURLConnection(url, "0");
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
		}
		return super.openConnection(url);
	}
}
