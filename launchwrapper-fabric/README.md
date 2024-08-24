Currently LaunchWrapper for Fabric is only supported in MultiMC derivatives.

## Instructions
- For Babric: obtain a [Babric PrismLauncher instance](https://github.com/babric/prism-instance)
- For OrnitheMC: use [Ornithe installer](https://maven.ornithemc.net/api/maven/latest/file/releases/net/ornithemc/ornithe-installer) and generate instance zip for preferred version
- Perform the same steps from [MultiMC guide](../MultiMC.md) for this instance
- Create another component with the following JSON:
    <details>
    <summary>Show JSON</summary>

    ```json
    {
        "formatVersion": 1,
        "name": "LaunchWrapper Fabric Compat",
        "uid": "org.mcphackers.launchwrapper.fabric",
        "version": "1.0-SNAPSHOT",
        "libraries": [
            {
                "downloads": {},
                "name": "org.mcphackers:launchwrapper-fabric:1.0-SNAPSHOT"
            }
        ],
        "mainClass": "org.mcphackers.launchwrapper.fabric.FabricBridge"
    }
    ```

    Again, for the library without download you need to build LaunchWrapper. Copy jar from `launchwrapper-fabric/build/libs/launchwrapper-fabric-1.0-SNAPSHOT.jar` to `<MutltiMC root>/libraries/org/mcphackers/launchwrapper-fabric/1.0-SNAPSHOT/launchwrapper-fabric-1.0-SNAPSHOT.jar`
    </details>

- Make sure the component is under LaunchWrapper component
