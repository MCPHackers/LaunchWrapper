package org.mcphackers.launchwrapper.tweak.injection.vanilla;

import org.mcphackers.launchwrapper.LaunchConfig;
import org.mcphackers.launchwrapper.tweak.injection.InjectionWithContext;
import org.mcphackers.launchwrapper.tweak.injection.MinecraftGetter;
import org.mcphackers.launchwrapper.util.ClassNodeSource;

public class OfflineSkins extends InjectionWithContext<MinecraftGetter> {

	public OfflineSkins(MinecraftGetter storage) {
		super(storage);
	}

	public String name() {
		return "Offline skins";
	}

	public boolean required() {
		return false;
	}

	public boolean apply(ClassNodeSource source, LaunchConfig config) {
		return true;
	}
}
