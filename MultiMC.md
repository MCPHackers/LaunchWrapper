# JSON patches for MultiMC instances
This allows changing the Minecraft version in the instance without re-editing the component, and easier interoperability with other MultiMC features. More information about JSON patches can be found [on the MultiMC wiki page about them](https://github.com/MultiMC/Launcher/wiki/JSON-Patches).

> [!IMPORTANT]
> Please obtain a build of [LaunchWrapper](https://github.com/MCPHackers/LaunchWrapper) And place it in `<MultiMC directory>/libraries/org/mcphackers/launchwrapper/1.0-SNAPSHOT/launchwrapper-1.0-SNAPSHOT.jar`, as it is currently not recommended to use versioned release. Use latest git commit whenever possible.

## Instructions
- Create a new MultiMC instance.
- Click "Edit Instance".
- Click "Add Empty". Enter "LaunchWrapper" for the name, and "org.mcphackers.launchwrapper" for the UUID.
- Select the new component labeled "LaunchWrapper", and click "Edit".
- Replace the contents of the file with something like this:
    <details>
    <summary>Show JSON</summary>

    ```json
    {
        "+traits": [
            "noapplet"
        ],
        "libraries": [
            {
                "downloads": {},
                "name": "org.mcphackers:launchwrapper:1.0-SNAPSHOT"
            },
            {
                "name": "org.ow2.asm:asm:9.7",
                "url": "https://repo1.maven.org/maven2/"
            },
            {
                "name": "org.ow2.asm:asm-tree:9.7",
                "url": "https://repo1.maven.org/maven2/"
            },
            {
                "name": "org.json:json:20240303",
                "url": "https://repo1.maven.org/maven2/"
            },
            {
                "name": "org.mcphackers.rdi:rdi:1.0",
                "url": "https://maven.glass-launcher.net/releases/"
            }
        ],
        "mainClass": "org.mcphackers.launchwrapper.Launch",
        "minecraftArguments": "--username ${auth_player_name} --session ${auth_session} --version ${version_name} --gameDir ${game_directory} --assetsDir ${assets_root} --assetIndex ${assets_index_name} --accessToken ${auth_access_token} --userType ${user_type} --versionType ${version_type}",
        "requires": [
            {
                "uid": "net.minecraft"
            }
        ],
        "formatVersion": 1,
        "name": "LaunchWrapper",
        "uid": "org.mcphackers.launchwrapper",
        "version": "1.0-SNAPSHOT"
    }
    ```

    </details>
    (If you're using Prism Launcher all previous steps can be skipped. Use "Import Components" button and locate the aforementioned JSON)

    For the library with empty `downloads` key you need to build LaunchWrapper manually using gradle wrapper. Just open command prompt and run `./gradlew build` in the root of this project, then put built library from `build/libs/launchwrapper-1.0-SNAPSHOT.jar` to `<MutltiMC root>/libraries/org/mcphackers/launchwrapper/1.0-SNAPSHOT/launchwrapper-1.0-SNAPSHOT.jar`

- Save and close the document.

