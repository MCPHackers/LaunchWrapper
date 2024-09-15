package org.mcphackers.launchwrapper.tweak;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.mcphackers.launchwrapper.LaunchConfig;
import org.mcphackers.launchwrapper.protocol.LegacyURLStreamHandler;
import org.mcphackers.launchwrapper.protocol.URLStreamHandlerProxy;
import org.mcphackers.launchwrapper.target.LaunchTarget;
import org.mcphackers.launchwrapper.target.MainLaunchTarget;
import org.mcphackers.launchwrapper.tweak.injection.Injection;
import org.mcphackers.launchwrapper.tweak.injection.legacy.AddMain;
import org.mcphackers.launchwrapper.tweak.injection.legacy.ClassicCrashScreen;
import org.mcphackers.launchwrapper.tweak.injection.legacy.FixClassicSession;
import org.mcphackers.launchwrapper.tweak.injection.legacy.FixGrayScreen;
import org.mcphackers.launchwrapper.tweak.injection.legacy.FixShutdown;
import org.mcphackers.launchwrapper.tweak.injection.legacy.FixSplashScreen;
import org.mcphackers.launchwrapper.tweak.injection.legacy.IndevSaving;
import org.mcphackers.launchwrapper.tweak.injection.legacy.LWJGLPatch;
import org.mcphackers.launchwrapper.tweak.injection.legacy.LegacyInit;
import org.mcphackers.launchwrapper.tweak.injection.legacy.LegacyTweakContext;
import org.mcphackers.launchwrapper.tweak.injection.legacy.OptionsLoadFix;
import org.mcphackers.launchwrapper.tweak.injection.legacy.ReplaceGameDir;
import org.mcphackers.launchwrapper.tweak.injection.legacy.UnlicensedCopyText;
import org.mcphackers.launchwrapper.tweak.injection.vanilla.ChangeBrand;
import org.mcphackers.launchwrapper.util.UnsafeUtils;
import org.mcphackers.launchwrapper.util.Util;

public class LegacyTweak extends Tweak {

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

	public LegacyTweak(LaunchConfig config) {
		super(config);
	}

	public List<Injection> getInjections() {
		return Arrays.asList(
			new LegacyInit(context),
			new ClassicCrashScreen(context),
			new UnlicensedCopyText(context),
			new FixSplashScreen(context),
			new FixGrayScreen(context),
			new FixShutdown(context),
			new IndevSaving(context),
			new LWJGLPatch(context),
			new ReplaceGameDir(context),
			new OptionsLoadFix(context),
			new FixClassicSession(context),
			new AddMain(context),
			new ChangeBrand()
		);
	}

	public List<LazyTweaker> getLazyTweakers() {
		return Collections.<LazyTweaker>singletonList(new Java5LazyTweaker());
	}

	private void downloadServer() {
		if(config.serverURL.get() == null || config.gameDir.get() == null) {
			return;
		}
		try {
			File serverFolder = new File(config.gameDir.get(), "server");
			File serverJar = new File(serverFolder, "minecraft_server.jar");
			String sha1 = null;
			if(serverJar.exists()) {
				sha1 = Util.getSHA1(new FileInputStream(serverJar));
			}
			if(config.serverSHA1.get() == null || !config.serverSHA1.get().equals(sha1)) {
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

	public LaunchTarget getLaunchTarget() {
		if(context.main == null) {
			return null;
		}
		downloadServer();
		enableLegacyMergeSort();
		URLStreamHandlerProxy.setURLStreamHandler("http", new LegacyURLStreamHandler(config));
		URLStreamHandlerProxy.setURLStreamHandler("https", new LegacyURLStreamHandler(config));
		MainLaunchTarget target = new MainLaunchTarget(context.minecraft.name);
		target.args = new String[] { config.username.get(), config.session.get() };
		return target;
	}
}
