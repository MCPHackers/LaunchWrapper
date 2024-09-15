package org.mcphackers.launchwrapper.micromixin;

import java.util.Collection;
import java.util.Collections;

import org.stianloader.micromixin.transform.api.MixinConfig;

public class SimpleMixinConfig extends MixinConfig {

    public SimpleMixinConfig(String mixinPackage, Collection<String> mixins) {
            super(
                true,
                null,
                mixinPackage,
                null,
                null,
                Collections.unmodifiableCollection(mixins),
                0,
                false,
                null,
                false,
                Collections.<String>emptyList(),
                Collections.<String>emptyList());
    }

}
