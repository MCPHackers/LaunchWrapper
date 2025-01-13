package org.mcphackers.launchwrapper.tweak.injection.vanilla;

import static org.mcphackers.launchwrapper.util.asm.InsnHelper.*;
import static org.objectweb.asm.Opcodes.*;

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

import org.mcphackers.launchwrapper.Launch;
import org.mcphackers.launchwrapper.LaunchConfig;
import org.mcphackers.launchwrapper.protocol.skin.Skin;
import org.mcphackers.launchwrapper.protocol.skin.SkinRequests;
import org.mcphackers.launchwrapper.protocol.skin.SkinTexture;
import org.mcphackers.launchwrapper.tweak.injection.InjectionWithContext;
import org.mcphackers.launchwrapper.tweak.injection.MinecraftGetter;
import org.mcphackers.launchwrapper.util.ClassNodeSource;
import org.mcphackers.launchwrapper.util.asm.NodeHelper;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

public class OfflineSkins extends InjectionWithContext<MinecraftGetter> {
	private static final SkinRequests SKINS;
	static {
		Launch launch = Launch.getInstance();
		if(launch == null) {
			SKINS = null;
		} else {
			LaunchConfig config = launch.config;
			SKINS = new SkinRequests(config.gameDir.get(), config.assetsDir.get(), config.skinOptions.get(), config.skinProxy.get());
		}
	}

	public OfflineSkins(MinecraftGetter storage) {
		super(storage);
	}

	public String name() {
		return "Offline skins";
	}

	public boolean required() {
		return false;
	}

	public boolean apply(ClassNodeSource source, LaunchConfig config) {
		MethodNode init = context.getInit();
		if (init == null) {
			return false;
		}
		ClassNode mc = context.getMinecraft();
		for (MethodNode m : NodeHelper.getConstructors(mc)) {
			AbstractInsnNode insn = getFirst(m.instructions);
			for (; insn != null; insn = nextInsn(insn)) {
				if (insn.getOpcode() != INVOKEVIRTUAL && insn.getOpcode() != INVOKESPECIAL) {
					continue;
				}
				MethodInsnNode mInsn = (MethodInsnNode)insn;
				if (compareInsn(mInsn, -1, "com/mojang/authlib/yggdrasil/YggdrasilAuthenticationService", "createMinecraftSessionService", "()Lcom/mojang/authlib/minecraft/MinecraftSessionService;")) {
					m.instructions.set(insn, new MethodInsnNode(INVOKESTATIC, "org/mcphackers/launchwrapper/tweak/injection/vanilla/OfflineSkins", "redirectSessionService", "(Lcom/mojang/authlib/yggdrasil/YggdrasilAuthenticationService;)Lcom/mojang/authlib/minecraft/MinecraftSessionService;"));
					source.overrideClass(mc);
					return true;
				}
			}
		}
		return false;
	}
	public static MinecraftSessionService redirectSessionService(YggdrasilAuthenticationService authenticationService) {
		final MinecraftSessionService dispatchTo = authenticationService.createMinecraftSessionService();
		return new MinecraftSessionService() {
			@Override
			public void joinServer(GameProfile profile, String authenticationToken, String serverId) throws AuthenticationException {
				dispatchTo.joinServer(profile, authenticationToken, serverId);
			}

			@Override
			public GameProfile hasJoinedServer(GameProfile user, String serverId, InetAddress address) throws AuthenticationUnavailableException {
				return dispatchTo.hasJoinedServer(user, serverId, address);
			}

			@Override
			public Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> getTextures(GameProfile profile, boolean requireSecure) {
				Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> map = new HashMap<MinecraftProfileTexture.Type, MinecraftProfileTexture>();
				String uuid = profile.getId().toString().replace("-", "");
				String name = profile.getName();
				Skin skin;
				skin = SKINS.getSkin(uuid, name, SkinTexture.SKIN);
				if (skin != null) {
					map.put(MinecraftProfileTexture.Type.SKIN, new MinecraftProfileTexture(skin.getURL(), skin.isSlim() ? Collections.singletonMap("model", "slim") : null));
				}
				skin = SKINS.getSkin(uuid, name, SkinTexture.CAPE);
				if (skin != null) {
					map.put(MinecraftProfileTexture.Type.CAPE, new MinecraftProfileTexture(skin.getURL(), null));
				}
				skin = SKINS.getSkin(uuid, name, SkinTexture.ELYTRA);
				if (skin != null) {
					map.put(MinecraftProfileTexture.Type.ELYTRA, new MinecraftProfileTexture(skin.getURL(), null));
				}
				return map;
			}

			@Override
			public GameProfile fillProfileProperties(GameProfile profile, boolean requireSecure) {
				return dispatchTo.fillProfileProperties(profile, requireSecure);
			}
		};
	}
}
