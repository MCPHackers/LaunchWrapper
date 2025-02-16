package org.mcphackers.launchwrapper.inject;

import java.net.InetAddress;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.exceptions.AuthenticationUnavailableException;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;

import org.mcphackers.launchwrapper.protocol.skin.Skin;
import org.mcphackers.launchwrapper.protocol.skin.SkinRequests;
import org.mcphackers.launchwrapper.protocol.skin.SkinTexture;

public final class SessionService implements MinecraftSessionService {
	public static MinecraftSessionService redirectSessionService(YggdrasilAuthenticationService authenticationService) {
		return new SessionService(authenticationService);
	}

	static final SkinRequests SKINS = SkinRequests.getInstance();
	final MinecraftSessionService dispatcher;

	private SessionService(YggdrasilAuthenticationService authenticationService) {
		dispatcher = authenticationService.createMinecraftSessionService();
	}

	@Override
	public void joinServer(GameProfile profile, String authenticationToken, String serverId) throws AuthenticationException {
		dispatcher.joinServer(profile, authenticationToken, serverId);
	}

	@Override
	public GameProfile hasJoinedServer(GameProfile user, String serverId, InetAddress address) throws AuthenticationUnavailableException {
		return dispatcher.hasJoinedServer(user, serverId, address);
	}

	@Override
	public Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> getTextures(GameProfile profile, boolean requireSecure) {
		if (SKINS == null) {
			return dispatcher.getTextures(profile, requireSecure);
		}
		Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> map = new HashMap<MinecraftProfileTexture.Type, MinecraftProfileTexture>();
		String uuid = profile.getId().toString().replace("-", "");
		String name = profile.getName();
		Skin skin;
		skin = SKINS.downloadSkin(uuid, name, SkinTexture.SKIN);
		if (skin != null) {
			map.put(MinecraftProfileTexture.Type.SKIN, new MinecraftProfileTexture(skin.getURL(), skin.isSlim() ? Collections.singletonMap("model", "slim") : null));
		}
		skin = SKINS.downloadSkin(uuid, name, SkinTexture.CAPE);
		if (skin != null) {
			map.put(MinecraftProfileTexture.Type.CAPE, new MinecraftProfileTexture(skin.getURL(), null));
		}
		skin = SKINS.downloadSkin(uuid, name, SkinTexture.ELYTRA);
		if (skin != null) {
			map.put(MinecraftProfileTexture.Type.ELYTRA, new MinecraftProfileTexture(skin.getURL(), null));
		}
		return map;
	}

	@Override
	public GameProfile fillProfileProperties(GameProfile profile, boolean requireSecure) {
		return dispatcher.fillProfileProperties(profile, requireSecure);
	}
}
