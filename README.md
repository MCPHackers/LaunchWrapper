# LaunchWrapper
![image](https://user-images.githubusercontent.com/68742864/227987160-03948674-48c2-4a69-b5b8-86793462f128.png)

**LaunchWrapper** is a wrapper for legacy Minecraft which provides fixes for certain game features and also improves compatibility with launchers.

LaunchWrapper is bundled in [BetterJSONs](https://github.com/MCPHackers/BetterJSONs) which are meant for official Minecraft Launcher.

## Supported versions
- All versions from rd-132211 to 1.12.2
- [Better Than Adventure](https://www.betterthanadventure.net/)
- Babric and OrnitheMC instances

## Features
- Does not depend on any hard-coded obfuscated names and is mostly compatible with every Minecraft version
	- This also includes modded versions
- **deAWT** (aka **M1Fix**) - Strips game window from Java **AWT** (Abstract Window Toolkit) and lets the game use internal **LWJGL** frame
- Replaces mouse input code with **LWJGL** calls (Fixes any mouse input issues in Classic, Indev and Infdev. Commonly included with deAWT or M1Fix mods)
- BitDepthFix
- Online mode authentication fix
- LaunchWrapper works with Risugami's Modloader so long as you're using Java 8
- Built-in [Modloader Fix](https://github.com/coffeenotfound/ModloaderFix-b1.7.3)
- Drop-in replacement for [LegacyLauncher (aka net.minecraft.launchwrapper)](https://github.com/Mojang/LegacyLauncher)
- Allows changing game directory in versions before 1.6
- Makes save slots in classic and indev functional and saves to `.minecraft/levels` directory
- Adds ability to launch classic and pre-classic at custom resolution or in fullscreen
- Fixes TimSort crash when using Java 8+
- **SkinFix** - Proxies all requests from old skin servers to the current skin API and converts skins to required format
	- Skins don't work in Java versions before 8u181, due to the requirement of TLS 1.2 support
- Adds ability to customize built-in **LWJGL** frame
	- Changing display title
	- Setting icon
- Fixes sounds by using sounds from `.minecraft/assets`
	- You need a valid asset index to be specified in `--assetIndex`
- Enabling VSync for versions without that option
- The wrapper is fully compatible with Java 5+ if the game is vanilla
	- The wrapper also fixes Beta 1.3, Pre-classic and Classic compatibility with Java 5
- Added parameter to automatically download `minecraft_server.jar` for snapshots: `12w18a`, `12w19a`, `12w21a`
- A launcher for isometric level viewer (IsomPreviewApplet)
- Allows changing assets directory in 1.6 snapshots (the game didn't have a parameter to do that yet)

## How to use
***There will be an installer eventually.***<br>
*For now, here is an in-depth guide for those who will be using the wrapper*

- If you are using MultiMC, [follow this guide](MultiMC.md).<br>
- If you are using Official Launcher (or any other launcher which follows the same versions folder structure), use [BetterJSONs](https://github.com/MCPHackers/BetterJSONs) and rename the JSONs if they are conflicting with any vanilla JSONs
- If you need to use these fixes with fabric read [this guide](launchwrapper-fabric/README.md)

### Setting up manually

Make sure all minecraft and wrapper libraries are on classpath<br>
Use `java -cp <classpath> org.mcphackers.launchwrapper.Launch <arguments>` to launch the game

Arguments are formatted the same way as in Mojang launch wrapper. <br>
For example: `--username DemoUser --width 1280 --height 720 --title "Minecraft Demo" --demo`

Arguments may be as follows:
- `awtFrame` - disable LWJGL frame patch and use AWT (Not recommended)
- `isom` - Launch IsomPreviewApplet
- `vsync` - Launch the game with VSync enabled
- `resizable` - Early Indev and Classic don't properly update viewport, so the wrapper disables frame resizing. To enable it anyway use this argument
- `skinProxy` - **pre-c0.0.19a** / **classic** / **pre-b1.9-pre4** / **pre-1.8** / **default** - convert the skin to one of these specified formats
	- **pre-c0.0.19a** - flatten all skin layers, flip bottom textures, mirror entire skin and crop to 64x32
	- **classic** - flatten all skin layers, flip bottom textures and crop to 64x32
	- **pre-b1.9-pre4** - flip bottom textures and crop to 64x32
	- **pre-1.8** - crop to 64x32
	- **default** - do nothing with requested skin
- `skinOptions` - list of optional skin convertion steps separated by ","
	- **ignoreAlex** - will not fill arm texture for slim models
	- **ignoreLayers** - will not flatten any body or hat layers
	- **removeHat** - removes head overlay
	- **useLeftArm** - left arm texture will be used when converting to 64x32 skin
	- **useLeftLeg** - left leg texture will be used when converting to 64x32 skin
- `assetsDir` - Will be used by LaunchWrapper to locate custom index.json
- `assetIndex` - Name of the `assetsDir`/indexes/index.json without .json extension which will be used
- `title` - The display title
- `icon` - List of paths of icon PNGs separated by file system path separator (";" on windows, ":" on unix)
- `applet` - Makes the game think you're running an applet which:
	- Removes quit button in versions between Beta 1.0 and release 1.5.2
	- Changes mouse input code in classic
- `survival` - Patches classic creative versions to start in survival
- `creative` - Patches classic survival versions to start in creative
- `oneSixFlag` - Toggles notice about the release of 1.6 in 1.5.2
- `serverSHA1` - Compare minecraft_server.jar in .minecraft/server against this hash
- `serverURL` - URL to download the server from if the hash is mismatched or the jar is missing
- \+ any [Minecraft launch arguments](https://wiki.vg/Launching_the_game#Game_Arguments) or applet parameters
