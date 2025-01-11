package org.mcphackers.launchwrapper.micromixin;

import java.util.Collection;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mcphackers.launchwrapper.micromixin.transformer.AccessWidenerImpl;
import org.mcphackers.launchwrapper.tweak.Tweaker;
import org.mcphackers.launchwrapper.tweak.injection.Injection;
import org.stianloader.micromixin.transform.api.MixinConfig;

public interface LaunchWrapperMod {
	@NotNull
	ModMetadata getMetadata();

	@Nullable
	MixinConfig getMixinConfig();

	@Nullable
	AccessWidenerImpl getAccessWidener();

	@NotNull
	List<Injection> getInjections();

	@NotNull
	List<Tweaker> getTweakers();

	/**
	 * Returns an Injection which should dynamically create mappings, starting from an unobfuscated entry point
	 * or you can return MappingSource.fromMappings(mappings) with predetermined mappings
	 * This method must return an instance of MappingSource with all class/method/field mappings used by mixins from getMixins()
	 * This instance should also be the first Injection in a list returned by {@link #getInjections()}
	 * @return Instance of MappingsSource
	 */
	@Nullable
	MappingSource getMappingSource();

	/**
	 * @return class names of all mod classes, mixin or not
	 */
	@NotNull
	Collection<String> getClasses();
}
