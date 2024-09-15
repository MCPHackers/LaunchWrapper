package org.mcphackers.launchwrapper.micromixin;

import java.util.Collection;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mcphackers.launchwrapper.micromixin.transformer.AccessWidenerImpl;
import org.stianloader.micromixin.transform.api.MixinConfig;

public interface MixinMod {
    @NotNull
    ModMetadata getMetadata();
    
    @NotNull
    MixinConfig getMixinConfig();
    
    @Nullable
    AccessWidenerImpl getAccessWidener();

    /**
     * Returns an Injection which should dynamically create mappings, starting from an unobfuscated entry point
     * or you can return MappingSource.fromMappings(mappings) with predetermined mappings
     * This method must return an instance of MappingSource with all class/method/field mappings used by mixins from getMixins()
     * @return Instance of MappingsSource
     */
    @Nullable
    MappingSource getMappingSource();

    /**
     * @return class names of all mod classes, mixin or not
     */
    Collection<String> getClasses();
}
