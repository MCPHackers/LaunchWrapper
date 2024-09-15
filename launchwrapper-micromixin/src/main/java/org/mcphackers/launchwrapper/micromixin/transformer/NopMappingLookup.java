package org.mcphackers.launchwrapper.micromixin.transformer;

import org.jetbrains.annotations.NotNull;
import org.stianloader.remapper.MappingLookup;
import org.stianloader.remapper.MappingSink;
import org.stianloader.remapper.MemberRef;

public class NopMappingLookup implements MappingLookup, MappingSink {

    @Override
    public @NotNull String getRemappedClassName(@NotNull String srcName) {
        return srcName;
    }

    @Override
    public @NotNull String getRemappedFieldName(@NotNull String srcOwner, @NotNull String srcName,
            @NotNull String srcDesc) {
        return srcName;
    }

    @Override
    public @NotNull String getRemappedMethodName(@NotNull String srcOwner, @NotNull String srcName,
            @NotNull String srcDesc) {
        return srcName;
    }

    @Override
    public @NotNull MappingSink remapClass(@NotNull String srcName, @NotNull String dstName) {
        return this;
    }

    @Override
    public @NotNull MappingSink remapMember(@NotNull MemberRef srcRef, @NotNull String dstName) {
        return this;
    }

}
