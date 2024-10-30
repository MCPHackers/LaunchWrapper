package org.mcphackers.launchwrapper.tweak;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.mcphackers.launchwrapper.LaunchConfig;
import org.mcphackers.launchwrapper.loader.LaunchClassLoader;
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
	protected ClassicCrashScreen crashPatch = new ClassicCrashScreen(context);

	public VanillaTweak(LaunchConfig launch) {
		super(launch);
	}

	@Override
	public List<Injection> getInjections() {
		return Arrays.<Injection>asList(
			context,
			crashPatch,
			new OutOfFocusFullscreen(context),
			new OneSixAssetsFix(context),
			new ChangeBrand());
	}

	@Override
	public List<Tweaker> getTweakers() {
		return Collections.<Tweaker>singletonList(new Java5Tweaker());
	}

	@Override
	public boolean handleError(LaunchClassLoader loader, Throwable t) {
		super.handleError(loader, t);
		return crashPatch.injectErrorAtInit(loader, t);
	}

	@Override
	public LaunchTarget getLaunchTarget() {
		URLStreamHandlerProxy.setURLStreamHandler("http", new LegacyURLStreamHandler(config));
		URLStreamHandlerProxy.setURLStreamHandler("https", new LegacyURLStreamHandler(config));
		return new MainLaunchTarget(MAIN_CLASS, context.args);
	}
}
