# LaunchWrapper
![image](https://user-images.githubusercontent.com/68742864/227984246-66e32041-fcf1-42bb-b451-6fe4e76c70a6.png)

**LaunchWrapper** is a wrapper for legacy Minecraft which provides fixes for certain game features and also improves compatibility with launchers.

LaunchWrapper is bundled in [BetterJSONs](https://github.com/MCPHackers/BetterJSONs) which are meant for official Minecraft Launcher.

## Features
- Strips game window from Java **AWT** (**Abstract Window Toolkit**) and lets the game use internal **LWJGL** frame
- BitDepthFix for versions before Beta 1.8
- Allows changing assets directory in 1.6 snapshots (the game didn't have a parameter to do that yet)
- Allows changing game directory in versions before 1.6
- Replaces mouse input code with **LWJGL** calls
- Fixes TimSort crash when using Java 8+
- Adds ability to customize built-in **LWJGL** frame
	- Changing display title
	- Setting icon
- Proxies all requests from old skin servers to the current skin API and converts skins to required format
	- Skins don't work in Java versions before 8u181, due to the requrement of TLS 1.2 support
- Uses Betacraft proxy to download sounds for versions before 1.6
- Adds ability to launch classic and pre-classic at custom resolution and fullscreen
- Makes save slots in classic and indev functional and saves to `.minecraft/levels` directory
- A launcher for isometric level viewer (IsomPreviewApplet)
- A VSync toggle (Used to prevent extreme framerate in early versions which causes **coil whine**)
- Does not depend on any hard-coded obfuscated names and is mostly compatible with every Minecraft version
	- This also includes modded versions
- A parameter to automatically download `minecraft_server.jar` for snapshots: `12w18a`, `12w19a`, `12w21a`
- The wrapper is fully compatible with Java 5+ if the game is vanilla
	- The wrapper also fixes Beta 1.3, Pre-classic and Classic compatibility with Java 5
- Built-in [Modloader Fix](https://github.com/coffeenotfound/ModloaderFix-b1.7.3)

## How to use
***There will be an installer eventually.***<br>
*For now, here is an in-depth guide for those who will be using the wrapper*

- If you are using MultiMC [follow this guide](MultiMC.md).<br>
- If you are using Official Launcher (or any other launcher which follows the same versions folder structure) use [BetterJSONs](https://github.com/MCPHackers/BetterJSONs) and rename the JSONs if they are conflicting with any vanilla JSONs

### Setting up manually

Make sure all minecraft and wrapper libraries are on classpath<br>
Use `java -cp <classpath> org.mcphackers.launchwrapper.Launch <arguments>` to launch the game

Arguments are formatted the same way as in Mojang launch wrapper. <br>
For example: `--username DemoUser --width 1280 --height 720 --title "Minecraft Demo" --demo`

Arguments may be as follows:
- `awtFrame` - disable LWJGL frame patch and use AWT
- `isom` - Launch IsomPreviewApplet
- `forceVsync` - Launch the game with VSync enabled
- `forceResizable` - Early Indev and Classic don't properly update viewport, so the wrapper disables frame resizing. To enable it anyway use this argument
- `skinProxy` - **pre-c0.0.19a** / **classic** / **pre-b1.9-pre4** / **pre-1.8** / **default** - convert the skin to one of these specified formats
	- **pre-c0.0.19a** - flatten all skin layers, flip bottom textures, mirror entire skin and crop to 64x32
	- **classic** - flatten all skin layers, flip bottom textures and crop to 64x32
	- **pre-b1.9-pre4** - flip bottom textures and crop to 64x32
	- **pre-1.8** - crop to 64x32
	- **default** - do nothing with requested skin
- `resourcesProxyPort` - Betacraft proxy port for sound resources
- `serverSHA1` - Compare minecraft_server.jar in .minecraft/server against this hash
- `serverURL` - URL to download the server from if the hash is mismatched or the jar is missing
- `icon` - List of paths of icon PNGs separated by `;`
- `title` - The display title
- `oneSixFlag` - Toggles notice about the release of 1.6 in 1.5.2
- \+ any [Minecraft launch arguments](https://wiki.vg/Launching_the_game#Game_Arguments) or applet parameters
