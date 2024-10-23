package org.mcphackers.launchwrapper.micromixin.transformer;

import org.jetbrains.annotations.NotNull;
import org.stianloader.remapper.MappingLookup;
import org.stianloader.remapper.MappingSink;
import org.stianloader.remapper.MemberRef;

public class MappingProvider implements MappingLookup, MappingSink {

	private final Mappings mappings;

	public MappingProvider(Mappings mappings) {
		this.mappings = mappings;
	}

	@Override
	public @NotNull String getRemappedClassName(@NotNull String srcName) {
		String ret = mappings.classes.get(srcName);
		if (ret == null) {
			return srcName;
		}
		return ret;
	}

	@Override
	public String getRemappedClassNameFast(@NotNull String srcName) {
		return mappings.classes.get(srcName);
	}

	@Override
	public @NotNull String getRemappedFieldName(@NotNull String srcOwner, @NotNull String srcName, @NotNull String srcDesc) {
		String ret = mappings.fields.get(new MemberRef(srcOwner, srcName, srcDesc));
		if (ret == null) {
			return srcName;
		}
		return ret;
	}

	@Override
	public @NotNull String getRemappedMethodName(@NotNull String srcOwner, @NotNull String srcName, @NotNull String srcDesc) {
		String ret = mappings.methods.get(new MemberRef(srcOwner, srcName, srcDesc));
		if (ret == null) {
			return srcName;
		}
		return ret;
	}

	@Override
	public @NotNull MappingSink remapClass(@NotNull String srcName, @NotNull String dstName) {
		mappings.classes.put(srcName, dstName);
		return this;
	}

	@Override
	public @NotNull MappingSink remapMember(@NotNull MemberRef srcRef, @NotNull String dstName) {
		if (srcRef.getDesc().contains("(")) {
			mappings.methods.put(srcRef, dstName);
		} else {
			mappings.fields.put(srcRef, dstName);
		}
		return this;
	}
}
