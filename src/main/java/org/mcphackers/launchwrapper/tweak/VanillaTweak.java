package org.mcphackers.launchwrapper.tweak;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.mcphackers.launchwrapper.LaunchConfig;
import org.mcphackers.launchwrapper.protocol.LegacyURLStreamHandler;
import org.mcphackers.launchwrapper.protocol.URLStreamHandlerProxy;
import org.mcphackers.launchwrapper.target.LaunchTarget;
import org.mcphackers.launchwrapper.target.MainLaunchTarget;
import org.mcphackers.launchwrapper.tweak.injection.Injection;
import org.mcphackers.launchwrapper.tweak.injection.legacy.ClassicCrashScreen;
import org.mcphackers.launchwrapper.tweak.injection.vanilla.ChangeBrand;
import org.mcphackers.launchwrapper.tweak.injection.vanilla.OneSixAssetsFix;
import org.mcphackers.launchwrapper.tweak.injection.vanilla.OutOfFocusFullscreen;
import org.mcphackers.launchwrapper.tweak.injection.vanilla.VanillaTweakContext;

public class VanillaTweak extends Tweak {
	public static final String MAIN_CLASS = "net/minecraft/client/main/Main";

	protected VanillaTweakContext context = new VanillaTweakContext();

	public VanillaTweak(LaunchConfig launch) {
		super(launch);
	}

	public List<Injection> getInjections() {
		return Arrays.<Injection>asList(
			context,
			new OutOfFocusFullscreen(context),
			new ClassicCrashScreen(context),
			new OneSixAssetsFix(context),
			new ChangeBrand()
		);
	}

	public LaunchTarget getLaunchTarget() {
		URLStreamHandlerProxy.setURLStreamHandler("http", new LegacyURLStreamHandler(config));
		URLStreamHandlerProxy.setURLStreamHandler("https", new LegacyURLStreamHandler(config));
		MainLaunchTarget target = new MainLaunchTarget(MAIN_CLASS, context.args);
		return target;
	}

	@Override
	public List<LazyTweaker> getLazyTweakers() {
		return Collections.emptyList();
	}

}
