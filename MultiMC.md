# How to use in MultiMC
- Make a new instance in MultiMC with the wanted version of Minecraft.
- Click `Edit Instance` - it should open the `Version` page of the instance.
- Select the `Minecraft` component and click `Customize` - `Edit`. If you have not configured an external Text Editor in MultiMC Settings, this will attempt to open your system JSON editor.
- If the opened JSON has the `legacyLaunch` trait (for Minecraft versions 1.5.2 and older), then add the `noapplet` trait after it as well.
- For Minecraft versions 1.5.2 and older, set up an empty `assetIndex` instead of the vanilla pre-1.6:
```json
"assetIndex": {
    "id": "empty",
    "sha1": "62ea787c1f800c091b98678b050453a5ae59d7bc",
    "size": 14,
    "totalSize": 0,
    "url": "https://mcphackers.github.io/assets/empty.json"
}
```
- If the opened JSON does not have a `libraries` array object (right after the `formatVersion` object), then add one. Add LaunchWrapper and its dependencies to the libraries to get an array like this:
```json
"libraries": [
    {
        "downloads": {
            "artifact": {
                "sha1": "120f6fdeb2602fa2d979ad0bdcd3c443a48148a6",
                "size": 107614,
                "url": "https://mcphackers.github.io/libraries/org/mcphackers/launchwrapper/1.0/launchwrapper-1.0.jar"
            }
        },
        "name": "org.mcphackers:launchwrapper:1.0"
    },
    {
        "downloads": {
            "artifact": {
                "sha1": "d96c99a30f5e1a19b0e609dbb19a44d8518ac01e",
                "size": 52660,
                "url": "https://repo.maven.apache.org/maven2/org/ow2/asm/asm-tree/9.2/asm-tree-9.2.jar"
            }
        },
        "name": "org.ow2.asm:asm-tree:9.2"
    },
    {
        "downloads": {
            "artifact": {
                "sha1": "81a03f76019c67362299c40e0ba13405f5467bff",
                "size": 122004,
                "url": "https://repo.maven.apache.org/maven2/org/ow2/asm/asm/9.2/asm-9.2.jar"
            }
        },
        "name": "org.ow2.asm:asm:9.2"
    },
    {
        "downloads": {
            "artifact": {
                "sha1": "d2c95f0a83b1b2b28131d46abfe8e68f7dc07ede",
                "size": 65667,
                "url": "https://mcphackers.github.io/libraries/org/json/json/20230311/json-20230311.jar"
            }
        },
        "name": "org.json:json:20230311"
    },
    {
        "downloads": {
            "artifact": {
                "sha1": "6fcff0b8d6173ac275f3728a9e5a987cfca88762",
                "size": 79599,
                "url": "https://mcphackers.github.io/libraries/org/mcphackers/rdi/rdi/1.0/rdi-1.0.jar"
            }
        },
        "name": "org.mcphackers.rdi:rdi:1.0"
    }
]
```
- If the opened JSON does not have a `mainClass` string object (right before the `mainJar` object), then add one. Set or change the `mainClass` value to `org.mcphackers.launchwrapper.Launch`.
- If the opened JSON does not have a `minecraftArguments` string object (right after the `mainJar` object), then add one. Set or change the `minecraftArguments` value to something like `--username ${auth_player_name} --session ${auth_session} --gameDir ${game_directory} --assetsDir ${game_assets}` (you can see examples below or in [BetterJSONs](https://mcphackers.github.io/BetterJSONs/)).

## Examples
*Base your JSON on [BetterJSONs](https://mcphackers.github.io/BetterJSONs/)*

### c0.30_01c
```json
{
    "+traits": [
        "legacyLaunch",
        "noapplet",
        "no-texturepacks"
    ],
    "appletClass": "com.mojang.minecraft.MinecraftApplet",
    "assetIndex": {
        "id": "empty",
        "sha1": "62ea787c1f800c091b98678b050453a5ae59d7bc",
        "size": 14,
        "totalSize": 0,
        "url": "https://mcphackers.github.io/assets/empty.json"
    },
    "formatVersion": 1,
    "libraries": [
        {
            "downloads": {
                "artifact": {
                    "sha1": "120f6fdeb2602fa2d979ad0bdcd3c443a48148a6",
                    "size": 107614,
                    "url": "https://mcphackers.github.io/libraries/org/mcphackers/launchwrapper/1.0/launchwrapper-1.0.jar"
                }
            },
            "name": "org.mcphackers:launchwrapper:1.0"
        },
        {
            "downloads": {
                "artifact": {
                    "sha1": "d96c99a30f5e1a19b0e609dbb19a44d8518ac01e",
                    "size": 52660,
                    "url": "https://repo.maven.apache.org/maven2/org/ow2/asm/asm-tree/9.2/asm-tree-9.2.jar"
                }
            },
            "name": "org.ow2.asm:asm-tree:9.2"
        },
        {
            "downloads": {
                "artifact": {
                    "sha1": "81a03f76019c67362299c40e0ba13405f5467bff",
                    "size": 122004,
                    "url": "https://repo.maven.apache.org/maven2/org/ow2/asm/asm/9.2/asm-9.2.jar"
                }
            },
            "name": "org.ow2.asm:asm:9.2"
        },
        {
            "downloads": {
                "artifact": {
                    "sha1": "d2c95f0a83b1b2b28131d46abfe8e68f7dc07ede",
                    "size": 65667,
                    "url": "https://mcphackers.github.io/libraries/org/json/json/20230311/json-20230311.jar"
                }
            },
            "name": "org.json:json:20230311"
        },
        {
            "downloads": {
                "artifact": {
                    "sha1": "855b05d238b72b03197e6702a765f344a32b8291",
                    "size": 79599,
                    "url": "https://mcphackers.github.io/libraries/org/mcphackers/rdi/rdi/1.0/rdi-1.0.jar"
                }
            },
            "name": "org.mcphackers.rdi:rdi:1.0"
        }
    ],
    "mainClass": "org.mcphackers.launchwrapper.Launch",
    "mainJar": {
        "downloads": {
            "artifact": {
                "sha1": "54622801f5ef1bcc1549a842c5b04cb5d5583005",
                "size": 297776,
                "url": "https://launcher.mojang.com/v1/objects/54622801f5ef1bcc1549a842c5b04cb5d5583005/client.jar"
            }
        },
        "name": "com.mojang:minecraft:c0.30_01c:client"
    },
    "minecraftArguments": "--username ${auth_player_name} --sessionid ${auth_session} --gameDir ${game_directory} --assetsDir ${game_assets} --resourcesProxyPort 11701 --skinProxy pre-b1.9-pre4",
    "name": "Minecraft",
    "releaseTime": "2009-12-22T00:00:00+02:00",
    "requires": [
        {
            "suggests": "2.9.4",
            "uid": "org.lwjgl"
        }
    ],
    "type": "old_alpha",
    "uid": "net.minecraft",
    "version": "c0.30_01c"
}

```

### a1.1.2_01
```json
{
    "+traits": [
        "legacyLaunch",
        "noapplet",
        "no-texturepacks"
    ],
    "assetIndex": {
        "id": "empty",
        "sha1": "62ea787c1f800c091b98678b050453a5ae59d7bc",
        "size": 14,
        "totalSize": 0,
        "url": "https://mcphackers.github.io/assets/empty.json"
    },
    "formatVersion": 1,
    "libraries": [
        {
            "downloads": {
                "artifact": {
                    "sha1": "120f6fdeb2602fa2d979ad0bdcd3c443a48148a6",
                    "size": 107614,
                    "url": "https://mcphackers.github.io/libraries/org/mcphackers/launchwrapper/1.0/launchwrapper-1.0.jar"
                }
            },
            "name": "org.mcphackers:launchwrapper:1.0"
        },
        {
            "downloads": {
                "artifact": {
                    "sha1": "d96c99a30f5e1a19b0e609dbb19a44d8518ac01e",
                    "size": 52660,
                    "url": "https://repo.maven.apache.org/maven2/org/ow2/asm/asm-tree/9.2/asm-tree-9.2.jar"
                }
            },
            "name": "org.ow2.asm:asm-tree:9.2"
        },
        {
            "downloads": {
                "artifact": {
                    "sha1": "81a03f76019c67362299c40e0ba13405f5467bff",
                    "size": 122004,
                    "url": "https://repo.maven.apache.org/maven2/org/ow2/asm/asm/9.2/asm-9.2.jar"
                }
            },
            "name": "org.ow2.asm:asm:9.2"
        },
        {
            "downloads": {
                "artifact": {
                    "sha1": "d2c95f0a83b1b2b28131d46abfe8e68f7dc07ede",
                    "size": 65667,
                    "url": "https://mcphackers.github.io/libraries/org/json/json/20230311/json-20230311.jar"
                }
            },
            "name": "org.json:json:20230311"
        },
        {
            "downloads": {
                "artifact": {
                    "sha1": "855b05d238b72b03197e6702a765f344a32b8291",
                    "size": 79599,
                    "url": "https://mcphackers.github.io/libraries/org/mcphackers/rdi/rdi/1.0/rdi-1.0.jar"
                }
            },
            "name": "org.mcphackers.rdi:rdi:1.0"
        }
    ],
    "mainClass": "org.mcphackers.launchwrapper.Launch",
    "mainJar": {
        "downloads": {
            "artifact": {
                "sha1": "daa4b9f192d2c260837d3b98c39432324da28e86",
                "size": 897164,
                "url": "https://launcher.mojang.com/v1/objects/daa4b9f192d2c260837d3b98c39432324da28e86/client.jar"
            }
        },
        "name": "com.mojang:minecraft:a1.1.2_01:client"
    },
    "minecraftArguments": "--username ${auth_player_name} --session ${auth_session} --gameDir ${game_directory} --assetsDir ${game_assets} --resourcesProxyPort 11702 --skinProxy pre-b1.9-pre4",
    "name": "Minecraft",
    "releaseTime": "2010-09-23T00:00:00+02:00",
    "requires": [
        {
            "suggests": "2.9.4",
            "uid": "org.lwjgl"
        }
    ],
    "type": "old_alpha",
    "uid": "net.minecraft",
    "version": "a1.1.2_01"
}

```

### b1.7.3
```json
{
    "+traits": [
        "legacyLaunch",
        "noapplet",
        "texturepacks"
    ],
    "assetIndex": {
        "id": "empty",
        "sha1": "62ea787c1f800c091b98678b050453a5ae59d7bc",
        "size": 14,
        "totalSize": 0,
        "url": "https://mcphackers.github.io/assets/empty.json"
    },
    "formatVersion": 1,
    "libraries": [
        {
            "downloads": {
                "artifact": {
                    "sha1": "120f6fdeb2602fa2d979ad0bdcd3c443a48148a6",
                    "size": 107614,
                    "url": "https://mcphackers.github.io/libraries/org/mcphackers/launchwrapper/1.0/launchwrapper-1.0.jar"
                }
            },
            "name": "org.mcphackers:launchwrapper:1.0"
        },
        {
            "downloads": {
                "artifact": {
                    "sha1": "d96c99a30f5e1a19b0e609dbb19a44d8518ac01e",
                    "size": 52660,
                    "url": "https://repo.maven.apache.org/maven2/org/ow2/asm/asm-tree/9.2/asm-tree-9.2.jar"
                }
            },
            "name": "org.ow2.asm:asm-tree:9.2"
        },
        {
            "downloads": {
                "artifact": {
                    "sha1": "81a03f76019c67362299c40e0ba13405f5467bff",
                    "size": 122004,
                    "url": "https://repo.maven.apache.org/maven2/org/ow2/asm/asm/9.2/asm-9.2.jar"
                }
            },
            "name": "org.ow2.asm:asm:9.2"
        },
        {
            "downloads": {
                "artifact": {
                    "sha1": "d2c95f0a83b1b2b28131d46abfe8e68f7dc07ede",
                    "size": 65667,
                    "url": "https://mcphackers.github.io/libraries/org/json/json/20230311/json-20230311.jar"
                }
            },
            "name": "org.json:json:20230311"
        },
        {
            "downloads": {
                "artifact": {
                    "sha1": "855b05d238b72b03197e6702a765f344a32b8291",
                    "size": 79599,
                    "url": "https://mcphackers.github.io/libraries/org/mcphackers/rdi/rdi/1.0/rdi-1.0.jar"
                }
            },
            "name": "org.mcphackers.rdi:rdi:1.0"
        }
    ],
    "mainClass": "org.mcphackers.launchwrapper.Launch",
    "mainJar": {
        "downloads": {
            "artifact": {
                "sha1": "43db9b498cb67058d2e12d394e6507722e71bb45",
                "size": 1465375,
                "url": "https://launcher.mojang.com/v1/objects/43db9b498cb67058d2e12d394e6507722e71bb45/client.jar"
            }
        },
        "name": "com.mojang:minecraft:b1.7.3:client"
    },
    "minecraftArguments": "--username ${auth_player_name} --session ${auth_session} --gameDir ${game_directory} --assetsDir ${game_assets} --resourcesProxyPort 11705 --skinProxy pre-b1.9-pre4",
    "name": "Minecraft",
    "releaseTime": "2011-07-08T00:00:00+02:00",
    "requires": [
        {
            "suggests": "2.9.4",
            "uid": "org.lwjgl"
        }
    ],
    "type": "old_beta",
    "uid": "net.minecraft",
    "version": "b1.7.3"
}

```

### 1.6.4
```json
{
    "assetIndex": {
        "id": "legacy",
        "sha1": "770572e819335b6c0a053f8378ad88eda189fc14",
        "size": 109634,
        "totalSize": 153475165,
        "url": "https://launchermeta.mojang.com/v1/packages/770572e819335b6c0a053f8378ad88eda189fc14/legacy.json"
    },
    "formatVersion": 1,
    "libraries": [
        {
            "downloads": {
                "artifact": {
                    "sha1": "6065cc95c661255349c1d0756657be17c29a4fd3",
                    "size": 61311,
                    "url": "https://libraries.minecraft.net/net/sf/jopt-simple/jopt-simple/4.5/jopt-simple-4.5.jar"
                }
            },
            "name": "net.sf.jopt-simple:jopt-simple:4.5"
        },
        {
            "downloads": {
                "artifact": {
                    "sha1": "4e61cd854bbd3d9765bd7c46fc9008d654b29281",
                    "size": 102767,
                    "url": "https://mcphackers.github.io/libraries/com/paulscode/codecjorbis/20230120/codecjorbis-20230120.jar"
                }
            },
            "name": "com.paulscode:codecjorbis:20230120"
        },
        {
            "downloads": {
                "artifact": {
                    "sha1": "12f031cfe88fef5c1dd36c563c0a3a69bd7261da",
                    "size": 5618,
                    "url": "https://libraries.minecraft.net/com/paulscode/codecwav/20101023/codecwav-20101023.jar"
                }
            },
            "name": "com.paulscode:codecwav:20101023"
        },
        {
            "downloads": {
                "artifact": {
                    "sha1": "5c5e304366f75f9eaa2e8cca546a1fb6109348b3",
                    "size": 21679,
                    "url": "https://libraries.minecraft.net/com/paulscode/libraryjavasound/20101123/libraryjavasound-20101123.jar"
                }
            },
            "name": "com.paulscode:libraryjavasound:20101123"
        },
        {
            "downloads": {
                "artifact": {
                    "sha1": "73e80d0794c39665aec3f62eee88ca91676674ef",
                    "size": 18981,
                    "url": "https://libraries.minecraft.net/com/paulscode/librarylwjglopenal/20100824/librarylwjglopenal-20100824.jar"
                }
            },
            "name": "com.paulscode:librarylwjglopenal:20100824"
        },
        {
            "downloads": {
                "artifact": {
                    "sha1": "419c05fe9be71f792b2d76cfc9b67f1ed0fec7f6",
                    "size": 65020,
                    "url": "https://libraries.minecraft.net/com/paulscode/soundsystem/20120107/soundsystem-20120107.jar"
                }
            },
            "name": "com.paulscode:soundsystem:20120107"
        },
        {
            "downloads": {
                "artifact": {
                    "sha1": "751761ce15a3e3aaf3fc75b9f013ff8f7b88a585",
                    "size": 74953,
                    "url": "https://libraries.minecraft.net/argo/argo/2.25_fixed/argo-2.25_fixed.jar"
                }
            },
            "name": "argo:argo:2.25_fixed"
        },
        {
            "downloads": {
                "artifact": {
                    "sha1": "b6f5d9926b0afbde9f4dbe3db88c5247be7794bb",
                    "size": 1997327,
                    "url": "https://libraries.minecraft.net/org/bouncycastle/bcprov-jdk15on/1.47/bcprov-jdk15on-1.47.jar"
                }
            },
            "name": "org.bouncycastle:bcprov-jdk15on:1.47"
        },
        {
            "downloads": {
                "artifact": {
                    "sha1": "67b7be4ee7ba48e4828a42d6d5069761186d4a53",
                    "size": 2189111,
                    "url": "https://libraries.minecraft.net/com/google/guava/guava/14.0/guava-14.0.jar"
                }
            },
            "name": "com.google.guava:guava:14.0"
        },
        {
            "downloads": {
                "artifact": {
                    "sha1": "905075e6c80f206bbe6cf1e809d2caa69f420c76",
                    "size": 315805,
                    "url": "https://libraries.minecraft.net/org/apache/commons/commons-lang3/3.1/commons-lang3-3.1.jar"
                }
            },
            "name": "org.apache.commons:commons-lang3:3.1"
        },
        {
            "downloads": {
                "artifact": {
                    "sha1": "b1b6ea3b7e4aa4f492509a4952029cd8e48019ad",
                    "size": 185140,
                    "url": "https://libraries.minecraft.net/commons-io/commons-io/2.4/commons-io-2.4.jar"
                }
            },
            "name": "commons-io:commons-io:2.4"
        },
        {
            "downloads": {
                "artifact": {
                    "sha1": "1f96456ca233dec780aa224bff076d8e8bca3908",
                    "size": 189285,
                    "url": "https://libraries.minecraft.net/com/google/code/gson/gson/2.2.2/gson-2.2.2.jar"
                }
            },
            "name": "com.google.code.gson:gson:2.2.2"
        },
        {
            "downloads": {
                "artifact": {
                    "sha1": "120f6fdeb2602fa2d979ad0bdcd3c443a48148a6",
                    "size": 107614,
                    "url": "https://mcphackers.github.io/libraries/org/mcphackers/launchwrapper/1.0/launchwrapper-1.0.jar"
                }
            },
            "name": "org.mcphackers:launchwrapper:1.0"
        },
        {
            "downloads": {
                "artifact": {
                    "sha1": "d96c99a30f5e1a19b0e609dbb19a44d8518ac01e",
                    "size": 52660,
                    "url": "https://repo.maven.apache.org/maven2/org/ow2/asm/asm-tree/9.2/asm-tree-9.2.jar"
                }
            },
            "name": "org.ow2.asm:asm-tree:9.2"
        },
        {
            "downloads": {
                "artifact": {
                    "sha1": "81a03f76019c67362299c40e0ba13405f5467bff",
                    "size": 122004,
                    "url": "https://repo.maven.apache.org/maven2/org/ow2/asm/asm/9.2/asm-9.2.jar"
                }
            },
            "name": "org.ow2.asm:asm:9.2"
        },
        {
            "downloads": {
                "artifact": {
                    "sha1": "d2c95f0a83b1b2b28131d46abfe8e68f7dc07ede",
                    "size": 65667,
                    "url": "https://mcphackers.github.io/libraries/org/json/json/20230311/json-20230311.jar"
                }
            },
            "name": "org.json:json:20230311"
        },
        {
            "downloads": {
                "artifact": {
                    "sha1": "855b05d238b72b03197e6702a765f344a32b8291",
                    "size": 79599,
                    "url": "https://mcphackers.github.io/libraries/org/mcphackers/rdi/rdi/1.0/rdi-1.0.jar"
                }
            },
            "name": "org.mcphackers.rdi:rdi:1.0"
        }
    ],
    "mainClass": "org.mcphackers.launchwrapper.Launch",
    "mainJar": {
        "downloads": {
            "artifact": {
                "sha1": "1703704407101cf72bd88e68579e3696ce733ecd",
                "size": 4745096,
                "url": "https://launcher.mojang.com/v1/objects/1703704407101cf72bd88e68579e3696ce733ecd/client.jar"
            }
        },
        "name": "com.mojang:minecraft:1.6.4:client"
    },
    "minecraftArguments": "--username ${auth_player_name} --session ${auth_session} --version ${version_name} --gameDir ${game_directory} --assetsDir ${game_assets} --skinProxy pre-1.8",
    "name": "Minecraft",
    "releaseTime": "2013-09-19T15:52:37+00:00",
    "requires": [
        {
            "suggests": "2.9.4",
            "uid": "org.lwjgl"
        }
    ],
    "type": "release",
    "uid": "net.minecraft",
    "version": "1.6.4"
}

```
