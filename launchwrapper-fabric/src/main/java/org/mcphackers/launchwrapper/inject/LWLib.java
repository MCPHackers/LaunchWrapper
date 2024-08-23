package org.mcphackers.launchwrapper.inject;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.impl.game.LibClassifier.LibraryType;

enum LWLib implements LibraryType {

    MC_CLIENT(EnvType.CLIENT, "net/minecraft/client/main/Main.class", "net/minecraft/client/Minecraft.class",
            "net/minecraft/client/MinecraftApplet.class", "com/mojang/minecraft/MinecraftApplet.class"),
    MC_SERVER(EnvType.SERVER, "net/minecraft/server/Main.class", "net/minecraft/server/MinecraftServer.class",
            "com/mojang/minecraft/server/MinecraftServer.class"),
    MODLOADER("ModLoader"),
    LAUNCHWRAPPER("org/mcphackers/launchwrapper/Launch.class"),
    LWJGL("org/lwjgl/Sys.class"),
    LWJGL3("org/lwjgl/Version.class");

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
