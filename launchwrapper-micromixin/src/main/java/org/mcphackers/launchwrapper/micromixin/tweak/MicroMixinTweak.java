package org.mcphackers.launchwrapper.micromixin.tweak;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.mcphackers.launchwrapper.Launch;
import org.mcphackers.launchwrapper.LaunchConfig;
import org.mcphackers.launchwrapper.loader.LaunchClassLoader;
import org.mcphackers.launchwrapper.micromixin.LaunchWrapperMod;
import org.mcphackers.launchwrapper.micromixin.LaunchWrapperModFile;
import org.mcphackers.launchwrapper.micromixin.tweak.injection.micromixin.MicroMixinInjection;
import org.mcphackers.launchwrapper.micromixin.tweak.injection.micromixin.PackageAccessFixer;
import org.mcphackers.launchwrapper.target.LaunchTarget;
import org.mcphackers.launchwrapper.tweak.Tweak;
import org.mcphackers.launchwrapper.tweak.Tweaker;
import org.mcphackers.launchwrapper.tweak.injection.Injection;
import org.mcphackers.launchwrapper.util.Util;

public class MicroMixinTweak extends Tweak {
	private static final boolean ACCESS_FIXER = Boolean.parseBoolean(System.getProperty("launchwrapper.accessfixer", "false"));
	public static final boolean DEV = Boolean.parseBoolean(System.getProperty("launchwrapper.dev", "false"));

	protected List<LaunchWrapperMod> mods = new ArrayList<LaunchWrapperMod>();
	protected Tweak baseTweak;
	protected MicroMixinTweaker microMixinTweaker = new MicroMixinTweaker();
	protected MicroMixinInjection microMixinInjection = new MicroMixinInjection(mods, microMixinTweaker);

	public MicroMixinTweak(Tweak tweak, LaunchConfig launch) {
		super(launch);
		if (tweak == null) {
			throw new NullPointerException("tweak mustn't be null");
		}
		baseTweak = tweak;
	}

	@Override
	public void prepare(LaunchClassLoader loader) {
		File modDir = new File(config.gameDir.get(), "mods");
		List<File> modFiles = new ArrayList<>();
		// Classpath mods have the highest priority
		modFiles.addAll(Util.getSource(loader, "launchwrapper.mod.json"));
		if (modDir.isDirectory()) {
			List<File> modList = Arrays.asList(modDir.listFiles());
			modList.sort(Comparator.naturalOrder());
			for (File f : modList) {
				if (!f.getName().endsWith(".jar")) {
					continue;
				}
				modFiles.add(f);
			}
		}
		for (File f : modFiles) {
			try {
				LaunchWrapperMod mod = new LaunchWrapperModFile(f);
				mods.add(mod);
			} catch (Exception e) {
				Launch.LOGGER.logDebug("WARNING: %s is not a mod: %s", f.getAbsolutePath(), e.getMessage());
			}
			try {
				if (f.exists()) {
					loader.addMod(f.toURI().toURL());
					Launch.LOGGER.log("Loading jarmod: %s", f.getAbsolutePath());
				}
			} catch (Exception e) {
				Launch.LOGGER.logErr("Failed to add to classpath: " + f.getAbsolutePath(), e);
			}
		}
	}

	/**
	 * @return List of mixin mods, which themselves contain mixin classes
	 */
	public List<LaunchWrapperMod> getMixins() {
		return mods;
	}

	@Override
	public List<Injection> getInjections() {
		List<LaunchWrapperMod> mods = getMixins();
		List<Injection> injects = new ArrayList<Injection>();
		for (LaunchWrapperMod mod : mods) {
			injects.addAll(mod.getInjections());
		}
		injects.addAll(baseTweak.getInjections());
		injects.add(microMixinInjection);
		return injects;
	}

	@Override
	public List<Tweaker> getTweakers() {
		List<Tweaker> list = new ArrayList<>();
		list.addAll(baseTweak.getTweakers());
		if (ACCESS_FIXER) {
			list.add(new PackageAccessFixer());
		}
		for (LaunchWrapperMod mod : mods) {
			list.addAll(mod.getTweakers());
		}
		list.add(microMixinTweaker);
		return list;
	}

	@Override
	public boolean handleError(LaunchClassLoader loader, Throwable t) {
		return baseTweak.handleError(loader, microMixinInjection.exception == null ? t : microMixinInjection.exception);
	}

	@Override
	public LaunchTarget getLaunchTarget() {
		return baseTweak.getLaunchTarget();
	}
}
