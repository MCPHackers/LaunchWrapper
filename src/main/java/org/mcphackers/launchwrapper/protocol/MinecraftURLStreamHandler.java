package org.mcphackers.launchwrapper.protocol;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

import org.mcphackers.launchwrapper.LaunchConfig;
import org.mcphackers.launchwrapper.protocol.skin.SkinRequests;

public class MinecraftURLStreamHandler extends URLStreamHandlerProxy {

	private final LaunchConfig config;
	private final AssetRequests assets;
	private final SkinRequests skins;
	private final File levelSaveDir;

	public MinecraftURLStreamHandler(LaunchConfig config) {
		this.config = config;
		this.assets = new AssetRequests(config.assetsDir.get(), config.assetIndex.get());
		this.skins = new SkinRequests(config.gameDir.get(), config.assetsDir.get(), config.skinOptions.get(), config.skinProxy.get());
		this.levelSaveDir = config.levelsDir.get();
	}

	@Override
	protected URLConnection openConnection(URL url) throws IOException {
		String host = url.getHost();
		String path = url.getPath();
		String file = url.getFile();
		if (host.endsWith(".minecraft.net") || host.equals("s3.amazonaws.com")) {
			if (path.equals("/game/joinserver.jsp"))
				return new JoinServerURLConnection(url, config.accessToken.get(), config.uuid.get());
			if (path.equals("/game/checkserver.jsp"))
				return new CheckServerURLConnection(url);
			if (path.equals("/login/session.jsp") || host.equals("login.minecraft.net") && path.equals("/session")) {
				// UnlicensedCopyText injection does this instead. (It doesn't fire the check thread for some reason)
				if (config.unlicensedCopy.get()) {
					return new BasicResponseURLConnection(url, 400, "");
				} else {
					return new BasicResponseURLConnection(url, "ok");
				}
			}
			if (path.equals("/game/"))
				return new BasicResponseURLConnection(url, "42069");
			if (path.contains("/level/save.html"))
				return new SaveLevelURLConnection(url, levelSaveDir);
			if (path.contains("/level/load.html"))
				return new LoadLevelURLConnection(url, levelSaveDir);
			if (path.equals("/listmaps.jsp"))
				return new ListLevelsURLConnection(url, levelSaveDir);
			if (path.startsWith("/MinecraftResources/") || path.startsWith("/resources/"))
				return new AssetURLConnection(url, assets);
			if (path.startsWith("/MinecraftSkins/") || path.startsWith("/skin/") || path.startsWith("/MinecraftCloaks/") || path.startsWith("/cloak/"))
				return new SkinURLConnection(url, skins);
			if (host.equals("assets.minecraft.net") && path.equals("/1_6_has_been_released.flag"))
				if (config.oneSixFlag.get())
					return new BasicResponseURLConnection(url, "https://web.archive.org/web/20130702232237if_/https://mojang.com/2013/07/minecraft-the-horse-update/");
				else
					return new BasicResponseURLConnection(url, "");

			// Telemetry
			if (host.equals("snoop.minecraft.net")) {
				if (path.equals("/client") || path.equals("/server"))
					return new BasicResponseURLConnection(url, "");
			}

			if (path.equals("/heartbeat.jsp"))
				return new HeartbeatURLConnection(url);
			if (host.equals("mcoapi.minecraft.net")) {
				if (config.realmsServer.get() != null) {
					return super.openConnection(new URL("http", config.realmsServer.get(), config.realmsPort.get(), file));
				} else {
					if (path.equals("/mco/available"))
						return new BasicResponseURLConnection(url, "true");
					if (path.equals("/mco/client/outdated"))
						return new BasicResponseURLConnection(url, "false");
				}
			}
		}
		// More telemetry (but modern)
		if (host.equals("api.minecraftservices.com") && path.equals("/events"))
			return new BasicResponseURLConnection(url, "");

		if (host.equals("skins.local"))
			return new LocalSkinURLConnection(url);

		return super.openConnection(url);
	}
}
