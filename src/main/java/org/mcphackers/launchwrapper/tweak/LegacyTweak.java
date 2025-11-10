package org.mcphackers.launchwrapper.tweak;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.imageio.ImageIO;

import org.mcphackers.launchwrapper.Launch;
import org.mcphackers.launchwrapper.LaunchConfig;
import org.mcphackers.launchwrapper.loader.LaunchClassLoader;
import org.mcphackers.launchwrapper.protocol.MinecraftURLStreamHandler;
import org.mcphackers.launchwrapper.protocol.URLStreamHandlerProxy;
import org.mcphackers.launchwrapper.target.AppletLaunchTarget;
import org.mcphackers.launchwrapper.target.LaunchTarget;
import org.mcphackers.launchwrapper.target.MainLaunchTarget;
import org.mcphackers.launchwrapper.tweak.injection.Injection;
import org.mcphackers.launchwrapper.tweak.injection.forge.ForgeVersionCheck;
import org.mcphackers.launchwrapper.tweak.injection.legacy.AddMain;
import org.mcphackers.launchwrapper.tweak.injection.legacy.BitDepthFix;
import org.mcphackers.launchwrapper.tweak.injection.legacy.ClassicCrashScreen;
import org.mcphackers.launchwrapper.tweak.injection.legacy.ClassicLoadingFix;
import org.mcphackers.launchwrapper.tweak.injection.legacy.FixGrayScreen;
import org.mcphackers.launchwrapper.tweak.injection.legacy.FixShutdown;
import org.mcphackers.launchwrapper.tweak.injection.legacy.FixSplashScreen;
import org.mcphackers.launchwrapper.tweak.injection.legacy.GameModeSwitch;
import org.mcphackers.launchwrapper.tweak.injection.legacy.IndevSaving;
import org.mcphackers.launchwrapper.tweak.injection.legacy.LWJGLPatch;
import org.mcphackers.launchwrapper.tweak.injection.legacy.LegacyTweakContext;
import org.mcphackers.launchwrapper.tweak.injection.legacy.MouseFix;
import org.mcphackers.launchwrapper.tweak.injection.legacy.OptionsLoadFix;
import org.mcphackers.launchwrapper.tweak.injection.legacy.ReplaceGameDir;
import org.mcphackers.launchwrapper.tweak.injection.legacy.UnlicensedCopyText;
import org.mcphackers.launchwrapper.tweak.injection.vanilla.ChangeBrand;
import org.mcphackers.launchwrapper.tweak.injection.vanilla.OutOfFocusFullscreen;
import org.mcphackers.launchwrapper.util.UnsafeUtils;
import org.mcphackers.launchwrapper.util.Util;
import org.mcphackers.launchwrapper.util.asm.NodeHelper;

public class LegacyTweak extends Tweak {

	public static final String MAIN_ISOM = "net/minecraft/isom/IsomPreviewApplet";

	public static final String[] MAIN_CLASSES = {
		"net/minecraft/client/Minecraft",
		"com/mojang/minecraft/Minecraft",
		"com/mojang/minecraft/RubyDung",
		"com/mojang/rubydung/RubyDung"
	};
	public static final String[] MAIN_APPLETS = {
		"net/minecraft/client/MinecraftApplet",
		"com/mojang/minecraft/MinecraftApplet"
	};
	protected LegacyTweakContext context = new LegacyTweakContext();
	protected ClassicCrashScreen crashPatch = new ClassicCrashScreen(context);

	public LegacyTweak(LaunchConfig config) {
		super(config);
	}

	@Override
	public List<Injection> getInjections() {
		return Arrays.asList(
			context,
			crashPatch,
			new ClassicLoadingFix(context),
			new UnlicensedCopyText(context),
			new FixSplashScreen(context),
			new FixGrayScreen(context),
			new FixShutdown(context),
			new IndevSaving(context),
			new BitDepthFix(context),
			new LWJGLPatch(context),
			new OutOfFocusFullscreen(context),
			new MouseFix(context),
			new ReplaceGameDir(context),
			new OptionsLoadFix(context),
			new GameModeSwitch(context),
			new AddMain(context),
			new ForgeVersionCheck(),
			new ChangeBrand());
	}

	@Override
	public List<Tweaker> getTweakers() {
		List<Tweaker> tweakers = new ArrayList<Tweaker>();
		tweakers.add(new Java5Tweaker());
		tweakers.add(new DelCharTweaker(config, context));
		if (config.useFakeApplet.get() || LaunchClassLoader.CLASS_VERSION >= 70 /* Java 26 */ ||
			Boolean.getBoolean("java.awt.headless")) {
			tweakers.add(new AppletTweaker());
		}
		return tweakers;
	}

	@Override
	public boolean handleError(LaunchClassLoader loader, Throwable t) {
		super.handleError(loader, t);
		if (config.isom.get()) {
			return false;
		}
		return crashPatch.injectErrorAtInit(loader, t);
	}

	protected AppletLaunchTarget getIsomLaunchTarget() {
		AppletLaunchTarget launchTarget = new AppletLaunchTarget(MAIN_ISOM.replace("/", "."));
		launchTarget.setTitle(config.title.get() != null ? config.title.get() : "IsomPreview");
		try {
			launchTarget.setIcon(ImageIO.read(Launch.class.getClassLoader().getResourceAsStream("favicon.png")));
		} catch (IOException e) {
			e.printStackTrace();
		}
		launchTarget.setResolution(config.width.get(), config.height.get());
		return launchTarget;
	}

	@Override
	public LaunchTarget getLaunchTarget() {
		if (config.isom.get()) {
			return getIsomLaunchTarget();
		}
		if (context.getMinecraft() == null || NodeHelper.getMethod(context.getMinecraft(), "main", "([Ljava/lang/String;)V") == null) {
			return null;
		}
		downloadServer();
		enableLegacyMergeSort();
		enableWLToolkit();
		if (LaunchClassLoader.CLASS_VERSION >= 64) {
			// Minecraft shows unknown unicode symbol in place of U+00A0 (NON-BREAKING SPACE) in dates, force old locale provider to use space
			System.setProperty("java.locale.providers", "SPI");
		}
		URLStreamHandlerProxy.setURLStreamHandler("http", new MinecraftURLStreamHandler(config));
		URLStreamHandlerProxy.setURLStreamHandler("https", new MinecraftURLStreamHandler(config));
		MainLaunchTarget target = new MainLaunchTarget(context.getMinecraft().name, new String[] { config.username.get(), config.session.get() });
		return target;
	}

	private void downloadServer() {
		if (config.serverURL.get() == null || config.gameDir.get() == null) {
			return;
		}
		try {
			File serverFolder = new File(config.gameDir.get(), "server");
			File serverJar = new File(serverFolder, "minecraft_server.jar");
			String sha1 = null;
			if (serverJar.exists()) {
				sha1 = Util.getSHA1(new FileInputStream(serverJar));
			}
			if (config.serverSHA1.get() == null || !config.serverSHA1.get().equals(sha1)) {
				URL url = new URL(config.serverURL.get());
				serverFolder.mkdirs();
				FileOutputStream fos = new FileOutputStream(serverJar);
				byte[] data = Util.readStream(url.openStream());
				fos.write(data);
				fos.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	protected void enableLegacyMergeSort() {
		try {
			Class<?> mergeSort = Class.forName("java.util.Arrays$LegacyMergeSort");
			Field userRequested = mergeSort.getDeclaredField("userRequested");
			UnsafeUtils.setStaticBoolean(userRequested, true);
		} catch (ClassNotFoundException e) {
		} catch (SecurityException e) {
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void enableWLToolkit() {
		try {
			Class.forName("sun.awt.wl.WLToolkit", false, Launch.class.getClassLoader());
			System.setProperty("awt.toolkit.name", "WLToolkit");
		} catch (ClassNotFoundException e) {
		}
	}
}
