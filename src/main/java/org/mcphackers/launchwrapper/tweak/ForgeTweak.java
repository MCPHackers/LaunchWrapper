package org.mcphackers.launchwrapper.tweak;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.mcphackers.launchwrapper.LaunchConfig;
import org.mcphackers.launchwrapper.loader.LaunchClassLoader;
import org.mcphackers.launchwrapper.tweak.injection.Injection;
import org.mcphackers.launchwrapper.tweak.injection.legacy.AddMain;
import org.mcphackers.launchwrapper.tweak.injection.legacy.ClassicCrashScreen;
import org.mcphackers.launchwrapper.tweak.injection.legacy.FixClassicSession;
import org.mcphackers.launchwrapper.tweak.injection.legacy.FixGrayScreen;
import org.mcphackers.launchwrapper.tweak.injection.legacy.FixShutdown;
import org.mcphackers.launchwrapper.tweak.injection.legacy.FixSplashScreen;
import org.mcphackers.launchwrapper.tweak.injection.legacy.ForgeVersionCheck;
import org.mcphackers.launchwrapper.tweak.injection.legacy.IndevSaving;
import org.mcphackers.launchwrapper.tweak.injection.legacy.LWJGLPatch;
import org.mcphackers.launchwrapper.tweak.injection.legacy.LegacyInit;
import org.mcphackers.launchwrapper.tweak.injection.legacy.OptionsLoadFix;
import org.mcphackers.launchwrapper.tweak.injection.legacy.ReplaceGameDir;
import org.mcphackers.launchwrapper.tweak.injection.legacy.UnlicensedCopyText;
import org.mcphackers.launchwrapper.tweak.injection.vanilla.ChangeBrand;

public class ForgeTweak extends LegacyTweak {

    public ForgeTweak(LaunchConfig config) {
        super(config);
    }

    public List<Injection> getInjections() {
		List<Injection> list = new ArrayList<Injection>();
        list.addAll(super.getInjections());
        list.add(new ForgeVersionCheck());
        return list;
	}

}
