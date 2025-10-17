package org.mcphackers.launchwrapper.tweak;

import java.security.CodeSource;
import java.util.ArrayList;
import java.util.List;

import org.mcphackers.launchwrapper.LaunchConfig;
import org.mcphackers.launchwrapper.loader.LaunchClassLoader;
import org.mcphackers.launchwrapper.target.LaunchTarget;
import org.mcphackers.launchwrapper.target.MainLaunchTarget;
import org.mcphackers.launchwrapper.tweak.injection.Injection;
import org.mcphackers.launchwrapper.tweak.injection.MinecraftGetter;
import org.mcphackers.launchwrapper.tweak.injection.fabric.FabricHook;

public class FabricLoaderTweak extends Tweak {
	private static final String FABRIC_KNOT_CLIENT = "net/fabricmc/loader/impl/launch/knot/KnotClient";
	private static final String FABRIC_KNOT = "net/fabricmc/loader/impl/launch/knot/Knot";

	protected Tweak baseTweak;

	public FabricLoaderTweak(Tweak baseTweak, LaunchConfig launch) {
		super(launch);
		this.baseTweak = baseTweak;
	}

	@Override
	public List<Injection> getInjections() {
		List<Injection> injects = new ArrayList<Injection>();
		List<Injection> baseInjections = baseTweak.getInjections();
		injects.addAll(baseInjections);
		for (Injection injection : baseInjections) {
			if (injection instanceof MinecraftGetter) {
				injects.add(new FabricHook((MinecraftGetter)injection));
				break;
			}
		}
		return injects;
	}

	@Override
	public List<Tweaker> getTweakers() {
		return baseTweak.getTweakers();
	}

	private static String gameProdiverSource() {
		// Location of META-INF/services/net.fabricmc.loader.impl.game.GameProvider
		CodeSource resource = FabricLoaderTweak.class.getProtectionDomain().getCodeSource();
		if (resource == null) {
			return null;
		}
		return resource.getLocation().getPath();
	}

	public Tweak getBaseTweak() {
		return baseTweak;
	}

	@Override
	public LaunchTarget getLaunchTarget() {
		MainLaunchTarget target = new MainLaunchTarget(FABRIC_KNOT_CLIENT, new String[0]) {
			@Override
			public void launch(LaunchClassLoader loader) {
				System.setProperty("fabric.skipMcProvider", "true");
				loader.overrideClassSource(FABRIC_KNOT, gameProdiverSource());
				loader.overrideClassSource(FABRIC_KNOT_CLIENT, gameProdiverSource());
				loader.invokeMain(FABRIC_KNOT_CLIENT.replace('/', '.'), config.getArgs());
			}
		};
		return target;
	}
}
