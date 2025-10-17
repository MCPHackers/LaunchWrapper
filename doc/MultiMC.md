# JSON patches for MultiMC/Prism Launcher instances
This allows changing the Minecraft version in the instance without re-editing the component, and easier interoperability with other MultiMC features. More information about JSON patches can be found [on the MultiMC wiki page about them](https://github.com/MultiMC/Launcher/wiki/JSON-Patches).

> [!NOTE]
> Overriding asset index requires a separate component and must be above "Minecraft" component

## Instructions for Vanilla
- Create a new MultiMC instance.
- Click "Edit Instance".
- Click "Add Empty". Enter "LaunchWrapper" for the name, and "org.mcphackers.launchwrapper" for the UID.
- Select the new component labeled "LaunchWrapper", and click "Edit".
- Replace the contents of the file with contents of [org.mcphackers.launchwrapper.json](org.mcphackers.launchwrapper.json)
- Save and close the document.
 
    (If you're using Prism Launcher all previous steps can be skipped. Use "Import Components" button and locate the aforementioned JSON)

> [!IMPORTANT] 
> Component order matters. Make sure LaunchWrapper is the bottom-most component

## Fabric
To install LaunchWrapper with Fabric, first you need to acquire a regular Fabric instance.
For this instance, perform the following steps:

- Repeat all the instructions for vanilla (but use another name and UID for the component when adding empty)
- Perform the same steps but instead of `org.mcphackers.launchwrapper.json` use the contents of [org.mcphackers.launchwrapper.fabric.json](org.mcphackers.launchwrapper.fabric.json)
- Make sure it's the bottom-most component and is below the regular "LaunchWrapper" component

Recent versions of Fabric loader will refuse to launch the game if multiple instances of asm libraries are present. You need to remove them.

Edit "LaunchWrapper" component and modify `libraries` array:

```diff
     "libraries": [
         {
             "name": "org.mcphackers:launchwrapper:1.2.2",
             "url": "https://maven.glass-launcher.net/releases/"
         },
-        {
-            "name": "org.ow2.asm:asm:9.9",
-            "url": "https://maven.fabricmc.net/"
-        },
-        {
-            "name": "org.ow2.asm:asm-tree:9.9",
-            "url": "https://maven.fabricmc.net/"
-        },
         {
             "name": "org.json:json:20250517",
             "url": "https://repo1.maven.org/maven2/"
         }
     ],
```
## Forge
To install LaunchWrapper with Forge follow instructions for Vanilla, then move "LaunchWrapper" component above Forge.

Remove asm libraries from LaunchWrapper component (refer to Fabric instructions)

This only applies to versions where Forge isn't considered a jarmod. For versions where Forge is a jarmod, use Vanilla instructions.
<!-- TODO: Determine version range -->

## Micromixin
Micromixin is a lightweight alternative mixin implementation which can be used with standalone LaunchWrapper. Currently the implementation is barebones so many mixin annotations aren't supported. However, if the mod uses exclusively the annotations from base Mixin (Not MixinExtras) there's a high chance it'll work when recompiled with
```groovy
loom {
    mixin {
        useLegacyMixinAp = false
    }
}
```
in gradle project of the mod.

LaunchWrapper Micromixin reads fabric.mod.json (aka FMJ) and attempts to load fabric mods with Micromixin. However there's also launchwrapper.mod.json which is used over FMJ if present.
JSON format differs. (TODO: No documentation yet)

To load fabric mods Intermediary mappings need to be present on classpath.

When loaded, fabric mods will be remapped to the "official" (obfuscated) namespace, which will keep compatibility with any base edit mods or reflection used by such mods.

To install Micromixin alongside LaunchWrapper perform same instructions as for vanilla component, but use the contents of [org.mcphackers.launchwrapper.micromixin.json](org.mcphackers.launchwrapper.micromixin.json). Make sure it's the bottom-most component and is below regular "LaunchWrapper" component