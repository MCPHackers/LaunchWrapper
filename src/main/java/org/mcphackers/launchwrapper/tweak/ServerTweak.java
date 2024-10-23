package org.mcphackers.launchwrapper.tweak;

import java.util.Collections;
import java.util.List;

import org.mcphackers.launchwrapper.LaunchConfig;
import org.mcphackers.launchwrapper.loader.LaunchClassLoader;
import org.mcphackers.launchwrapper.protocol.LegacyURLStreamHandler;
import org.mcphackers.launchwrapper.protocol.URLStreamHandlerProxy;
import org.mcphackers.launchwrapper.target.LaunchTarget;
import org.mcphackers.launchwrapper.tweak.injection.Injection;

public class ServerTweak extends Tweak {
	public static final String[] MAIN_CLASSES = {
		"net/minecraft/server/MinecraftServer",
		"com/mojang/minecraft/server/MinecraftServer"
	};

	public ServerTweak(LaunchConfig config) {
		super(config);
	}

	@Override
	public List<Injection> getInjections() {
		return Collections.emptyList();
	}

	@Override
	public List<Tweaker> getTweakers() {
		// return Collections.<LazyTweaker>singletonList(new Java5LazyTweaker());
		return Collections.emptyList();
	}

	@Override
	public LaunchTarget getLaunchTarget() {
		URLStreamHandlerProxy.setURLStreamHandler("http", new LegacyURLStreamHandler(config));
		URLStreamHandlerProxy.setURLStreamHandler("https", new LegacyURLStreamHandler(config));
		return new LaunchTarget() {
			public void launch(LaunchClassLoader classLoader) {
				for (String main : MAIN_CLASSES) {
					if (classLoader.getClass(main) != null) {
						classLoader.invokeMain(main.replace('/', '.'), config.getArgs());
						return;
					}
				}
			}
		};
	}
}
