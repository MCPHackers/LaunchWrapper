package net.minecraft.launchwrapper;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.mcphackers.launchwrapper.LaunchConfig;
import org.mcphackers.launchwrapper.target.LaunchTarget;
import org.mcphackers.launchwrapper.target.MainLaunchTarget;
import org.mcphackers.launchwrapper.tweak.Tweak;
import org.mcphackers.launchwrapper.tweak.injection.forge.ForgeFix;

@Deprecated
public final class Launch extends org.mcphackers.launchwrapper.Launch {
	public static File minecraftHome;
	public static File assetsDir;
	public static Map<String, Object> blackboard = new HashMap<String, Object>();

	public static LaunchClassLoader classLoader;

	private final List<String> arguments = new ArrayList<String>();
	private ITweaker primaryTweaker;

	private Launch(LaunchConfig config) {
		super(config);
	}

	public static void main(String[] args) {
		LaunchConfig config = new LaunchConfig(args);
		new Launch(config).launch();
	}

	@Override
	protected Tweak getTweak() {
		return Tweak.getDefault(getLoader(), config);
	}

	@Override
	public LaunchClassLoader getLoader() {
		if (classLoader == null) {
			classLoader = LaunchClassLoader.instantiate();
		}
		return classLoader;
	}

	@Override
	public void launch() {
		List<String> tweakClassNames = new ArrayList<String>();
		if (config.tweakClass.get() != null) {
			tweakClassNames.add(config.tweakClass.get());
		}
		blackboard.put("TweakClasses", tweakClassNames);
		blackboard.put("ArgumentList", new ArrayList<String>());
		minecraftHome = config.gameDir.get();
		assetsDir = config.assetsDir.get();
		LaunchClassLoader loader = getLoader();
		runPreInitTweaks(loader);
		runTweakers(loader);
		Tweak mainTweak = getTweak();
		if (mainTweak == null) {
			if (config.tweakClass.get() == null) {
				LOGGER.logErr("No suitable tweak found. Is Minecraft on classpath?");
			} else {
				LOGGER.logErr("Specified tweak does not exist.");
			}
			return;
		}
		mainTweak.prepare(loader);
		try {
			if (!mainTweak.transform(loader)) {
				LOGGER.logErr("Tweak could not be applied");
				if (!mainTweak.handleError(loader, null)) { // if handled successfully, continue
					return;
				}
			}
		} catch (Throwable t) {
			LOGGER.logErr("Tweak could not be applied", t);
			if (!mainTweak.handleError(loader, t)) {
				return;
			}
		}
		mainTweak.transformResources(loader);
		getLaunchTarget(mainTweak.getLaunchTarget()).launch(loader);
	}

	private LaunchTarget getLaunchTarget(LaunchTarget launchTarget) {
		if (primaryTweaker == null) {
			return launchTarget;
		}
		String target = primaryTweaker.getLaunchTarget();
		LOGGER.log("Launching wrapped minecraft {%s}", target);
		return new MainLaunchTarget(target, arguments.toArray(new String[arguments.size()]));
	}

	private List<String> getArgs() {
		List<String> args = new ArrayList<String>();
		LaunchConfig configCopy = config.clone();
		configCopy.gameDir.set(null);
		configCopy.assetsDir.set(null);
		configCopy.version.set(null);
		for (Map.Entry<String, String> entry : configCopy.getArgsAsMap().entrySet()) {
			args.add("--" + entry.getKey());
			args.add(entry.getValue());
		}
		return args;
	}

	private void runPreInitTweaks(LaunchClassLoader loader) {
		new ForgeFix().apply(loader, config);
	}

	@SuppressWarnings("unchecked")
	private void runTweakers(LaunchClassLoader loader) {
		List<String> args = getArgs();
		List<String> tweakClassNames = (List<String>)blackboard.get("TweakClasses");

		final Set<String> allTweakerNames = new HashSet<String>();
		final List<ITweaker> allTweakers = new ArrayList<ITweaker>();
		try {
			final List<ITweaker> tweakers = new ArrayList<ITweaker>(tweakClassNames.size() + 1);
			blackboard.put("Tweaks", tweakers);
			do {
				for (final Iterator<String> it = tweakClassNames.iterator(); it.hasNext();) {
					final String tweakName = it.next();
					if (allTweakerNames.contains(tweakName)) {
						LOGGER.log("Tweak class name %s has already been visited -- skipping", tweakName);
						it.remove();
						continue;
					} else {
						allTweakerNames.add(tweakName);
					}
					LOGGER.log("Loading tweak class name %s", tweakName);

					loader.addClassLoaderExclusion(tweakName.substring(0, tweakName.lastIndexOf('.')));
					final ITweaker tweaker = (ITweaker)Class.forName(tweakName, true, loader).newInstance();
					tweakers.add(tweaker);

					it.remove();
					if (primaryTweaker == null) {
						LOGGER.log("Using primary tweak class name %s", tweakName);
						primaryTweaker = tweaker;
					}
				}

				for (final Iterator<ITweaker> it = tweakers.iterator(); it.hasNext();) {
					final ITweaker tweaker = it.next();
					LOGGER.log("Calling tweak class %s", tweaker.getClass().getName());
					tweaker.acceptOptions(args, config.gameDir.get(), config.assetsDir.get(), config.version.get());
					tweaker.injectIntoClassLoader(loader);
					allTweakers.add(tweaker);
					it.remove();
				}
			} while (!tweakClassNames.isEmpty());
			for (final ITweaker tweaker : allTweakers) {
				arguments.addAll(Arrays.asList(tweaker.getLaunchArguments()));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
