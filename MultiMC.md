# JSON patches for MultiMC instances
This allows changing the Minecraft version in the instance without re-editing the component, and easier interoperability with other MultiMC features. More information about JSON patches can be found [on the MultiMC wiki page about them](https://github.com/MultiMC/Launcher/wiki/JSON-Patches). Here is a [MultiMC instance](https://github.com/MCPHackers/LaunchWrapper/files/11588554/LaunchWrapperMCP-MultiMC.zip) demonstrating this.

Instructions:
- Create a new MultiMC instance.
- Click "Edit Instance".
- Click "Add Empty". Enter "LaunchWrapper" for the name, and "org.mcphackers.launchwrapper" for the UUID.
- Select the new component labeled "LaunchWrapper", and click "Edit".
- Replace the contents of the file with something like this:

 ```json
{
    "+traits": [
        "noapplet"
    ],
    "assetIndex": {
        "id": "empty",
        "sha1": "62ea787c1f800c091b98678b050453a5ae59d7bc",
        "size": 14,
        "totalSize": 0,
        "url": "https://mcphackers.github.io/assets/empty.json"
    },
    "libraries": [
        {
            "name": "org.mcphackers:launchwrapper:1.0",
            "url": "https://mcphackers.github.io/libraries/"
        },
        {
            "name": "org.ow2.asm:asm:9.2",
            "url": "https://repo1.maven.org/maven2/"
        },
        {
            "name": "org.ow2.asm:asm-tree:9.2",
            "url": "https://repo1.maven.org/maven2/"
        },
        {
            "name": "org.json:json:20230311",
            "url": "https://mcphackers.github.io/libraries/"
        },
        {
            "name": "org.mcphackers.rdi:rdi:1.0",
            "url": "https://mcphackers.github.io/libraries/"
        }
    ],
    "mainClass": "org.mcphackers.launchwrapper.Launch",
    "minecraftArguments": "--username ${auth_player_name} --session ${auth_session} --gameDir ${game_directory} --assetsDir ${game_assets}",
    "requires": [
        {
            "uid": "net.minecraft"
        }
    ],
    "formatVersion": 1,
    "name": "LaunchWrapper",
    "uid": "org.mcphackers.launchwrapper",
    "version": "1.0.0"
}
```

 The existing version-specific information about creating a MultiMC instance should apply here (`minecraftArguments` should be customised as needed, `assetIndex` and `noapplet` can be removed for Minecraft 1.6 and onwards etc). The main difference between this and the existing instructions is that the format of the `libraries` array is different for JSON patches (`url` is treated like a maven repo).

- Save and close the document.

This should produce an instance similar to the attached one. I also edited mmc-pack.json to set `dependencyOnly` to be true on the LaunchWrapper patch, which prevents disabling it (optional).
