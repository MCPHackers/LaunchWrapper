package org.mcphackers.launchwrapper.inject;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.impl.game.LibClassifier.LibraryType;

enum LWLib implements LibraryType {

	MC_CLIENT(EnvType.CLIENT, "net/minecraft/client/main/Main.class", "net/minecraft/client/MinecraftApplet.class", "com/mojang/minecraft/MinecraftApplet.class", "com/mojang/minecraft/RubyDung.class", "com/mojang/rubydung/RubyDung.class"),
	MC_SERVER(EnvType.SERVER, "net/minecraft/server/Main.class", "net/minecraft/server/MinecraftServer.class", "com/mojang/minecraft/server/MinecraftServer.class"),
	MC_COMMON("net/minecraft/server/MinecraftServer.class"),
	MODLOADER("ModLoader.class"),
	LAUNCHWRAPPER("org/mcphackers/launchwrapper/Launch.class"),
	MC_BUNDLER(EnvType.SERVER, "net/minecraft/bundler/Main.class"),
	REALMS(EnvType.CLIENT, "realmsVersion", "com/mojang/realmsclient/RealmsVersion.class"),
	LWJGL("org/lwjgl/Sys.class"),
	LWJGL3("org/lwjgl/Version.class"),

	LOG4J_API("org/apache/logging/log4j/LogManager.class"),
	LOG4J_CORE("META-INF/services/org.apache.logging.log4j.spi.Provider", "META-INF/log4j-provider.properties"),
	LOG4J_CONFIG("log4j2.xml"),
	LOG4J_PLUGIN("com/mojang/util/UUIDTypeAdapter.class"), // in authlib
	LOG4J_PLUGIN_2("com/mojang/patchy/LegacyXMLLayout.class"), // in patchy
	LOG4J_PLUGIN_3("net/minecrell/terminalconsole/util/LoggerNamePatternSelector.class"), // in terminalconsoleappender, used by loom's log4j config
	GSON("com/google/gson/TypeAdapter.class"), // used by log4j plugins
	SLF4J_API("org/slf4j/Logger.class"),
	SLF4J_CORE("META-INF/services/org.slf4j.spi.SLF4JServiceProvider");

	static final LWLib[] GAME = { MC_CLIENT, MC_SERVER, MC_BUNDLER };
	static final LWLib[] LOGGING = { LOG4J_API, LOG4J_CORE, LOG4J_CONFIG, LOG4J_PLUGIN, LOG4J_PLUGIN_2, LOG4J_PLUGIN_3, GSON, SLF4J_API, SLF4J_CORE };

	private final EnvType env;
	private final String[] paths;

	LWLib(String path) {
		this(null, new String[] { path });
	}

	LWLib(String... paths) {
		this(null, paths);
	}

	LWLib(EnvType env, String... paths) {
		this.paths = paths;
		this.env = env;
	}

	@Override
	public boolean isApplicable(EnvType env) {
		return this.env == null || this.env == env;
	}

	@Override
	public String[] getPaths() {
		return paths;
	}
}
