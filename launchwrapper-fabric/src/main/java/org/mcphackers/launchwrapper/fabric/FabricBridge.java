package org.mcphackers.launchwrapper.fabric;

import java.security.CodeSource;

import org.mcphackers.launchwrapper.Launch;
import org.mcphackers.launchwrapper.LaunchConfig;
import org.mcphackers.launchwrapper.loader.LaunchClassLoader;

public class FabricBridge extends Launch {
	private static final String FABRIC_KNOT_CLIENT = "net/fabricmc/loader/impl/launch/knot/KnotClient";
	private static final String FABRIC_KNOT = "net/fabricmc/loader/impl/launch/knot/Knot";

	private static FabricBridge INSTANCE;

	protected FabricBridge(LaunchConfig config) {
		super(config);
		INSTANCE = this;
	}

	private static String gameProdiverSource() {
		// Location of META-INF/services/net.fabricmc.loader.impl.game.GameProvider
		CodeSource resource = FabricBridge.class.getProtectionDomain().getCodeSource();
		if (resource == null) {
			return null;
		}
		Launch.LOGGER.logDebug("Fabric compat jar: " + resource.getLocation().getPath());
		return resource.getLocation().getPath();
	}

	public static void main(String[] args) {
		LaunchConfig config = new LaunchConfig(args);
		create(config).launch();
	}

	public void launch() {
		LaunchClassLoader loader = getLoader();
		loader.overrideClassSource(FABRIC_KNOT, gameProdiverSource());
		loader.overrideClassSource(FABRIC_KNOT_CLIENT, gameProdiverSource());
		loader.invokeMain(FABRIC_KNOT_CLIENT.replace('/', '.'), config.getArgs());
	}

	public static FabricBridge getInstance() {
		return INSTANCE;
	}

	public static FabricBridge create(LaunchConfig config) {
		return new FabricBridge(config);
	}
}
