package org.mcphackers.launchwrapper.micromixin.tweak.injection.micromixin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import net.fabricmc.accesswidener.AccessWidener;
import net.fabricmc.accesswidener.AccessWidenerClassVisitor;

import org.jetbrains.annotations.NotNull;
import org.mcphackers.launchwrapper.Launch;
import org.mcphackers.launchwrapper.LaunchConfig;
import org.mcphackers.launchwrapper.micromixin.LaunchWrapperMod;
import org.mcphackers.launchwrapper.micromixin.MappingSource;
import org.mcphackers.launchwrapper.micromixin.ModMetadata;
import org.mcphackers.launchwrapper.micromixin.transformer.AccessWidenerImpl;
import org.mcphackers.launchwrapper.micromixin.transformer.MappingProvider;
import org.mcphackers.launchwrapper.micromixin.transformer.Mappings;
import org.mcphackers.launchwrapper.micromixin.transformer.MemberLookup;
import org.mcphackers.launchwrapper.micromixin.transformer.NopMappingLookup;
import org.mcphackers.launchwrapper.micromixin.transformer.RemappedMemberLookup;
import org.mcphackers.launchwrapper.micromixin.tweak.MicroMixinTweaker;
import org.mcphackers.launchwrapper.tweak.injection.Injection;
import org.mcphackers.launchwrapper.util.ClassNodeSource;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.stianloader.micromixin.remapper.MemberLister;
import org.stianloader.micromixin.remapper.MicromixinRemapper;
import org.stianloader.micromixin.transform.api.MixinConfig;
import org.stianloader.micromixin.transform.api.MixinTransformer;
import org.stianloader.micromixin.transform.api.supertypes.ASMClassWrapperProvider;
import org.stianloader.micromixin.transform.api.supertypes.ClassWrapperPool;
import org.stianloader.remapper.HierarchyAwareMappingDelegator;
import org.stianloader.remapper.Remapper;

public class MicroMixinInjection implements Injection {

	private Collection<LaunchWrapperMod> mixinMods;
	private MicroMixinTweaker mixinTweaker;

	public Exception exception;

	public MicroMixinInjection(Collection<LaunchWrapperMod> mixinMods, MicroMixinTweaker mixinTweaker) {
		this.mixinMods = mixinMods;
		this.mixinTweaker = mixinTweaker;
	}

	@Override
	public String name() {
		return "Micromixin";
	}

	@Override
	public boolean required() {
		return true;
	}

	private ClassWrapperPool getPool(ClassNodeSource source) {
		return new ClassWrapperPool(Collections.singletonList(new ASMClassWrapperProvider() {
			public ClassNode getNode(@NotNull String name) {
				return source.getClass(name);
			}
		}));
	}

	@Override
	public boolean apply(ClassNodeSource source, LaunchConfig config) {
		final MixinTransformer<ClassNodeSource> transformer = new MixinTransformer<>((src, name) -> src.getClass(name), getPool(source));
		mixinTweaker.transformer = transformer;

		for (LaunchWrapperMod mod : mixinMods) {
			ModMetadata metadata = mod.getMetadata();
			Launch.LOGGER.log("Loading mod: %s %s", metadata.name, metadata.version);
			MixinConfig mixinConfig = mod.getMixinConfig();
			if (mixinConfig == null) {
				continue;
			}
			List<ClassNode> mixins = new ArrayList<>();
			for (String mixin : mixinConfig.mixins) {
				ClassNode mixinNode = source.getClass(mixinConfig.mixinPackage + "/" + mixin);
				if (mixinNode != null) {
					mixins.add(mixinNode);
				} else {
					Launch.LOGGER.log("WARNING: Missing mixin - " + mixinConfig.mixinPackage + "/" + mixin);
				}
			}

			MappingSource mappingSource = mod.getMappingSource();
			MemberLister memberLister;
			HierarchyAwareMappingDelegator<?> mappingProvider;
			if (mappingSource != null) {
				Mappings mappings = mappingSource.getMappings();
				RemappedMemberLookup remappedMemberLookup = new RemappedMemberLookup(source, mappings);
				memberLister = remappedMemberLookup;
				mappingProvider = new HierarchyAwareMappingDelegator<MappingProvider>(new MappingProvider(mappings), remappedMemberLookup);
			} else {
				MemberLookup classNodeMemberLookup = new MemberLookup(source);
				memberLister = classNodeMemberLookup;
				mappingProvider = new HierarchyAwareMappingDelegator<NopMappingLookup>(new NopMappingLookup(), classNodeMemberLookup);
			}

			AccessWidenerImpl awImpl = mod.getAccessWidener();
			if (awImpl != null) {
				AccessWidener aw = awImpl.getRemappedAccessWidener(mappingProvider);
				for (String target : aw.getTargets()) {
					ClassNode targetNode = source.getClass(target.replace('.', '/'));
					if (targetNode == null) {
						continue;
					}
					ClassNode accessibleNode = new ClassNode();
					ClassVisitor visitor = AccessWidenerClassVisitor.createClassVisitor(Opcodes.ASM9, accessibleNode, aw);
					targetNode.accept(visitor);
					source.overrideClass(accessibleNode);
				}
			}
			if (mappingSource != null) {
				MicromixinRemapper mixinRemapper = new MicromixinRemapper(mappingProvider,
																		  mappingProvider, memberLister);

				Remapper nodeRemapper = new Remapper(mappingProvider);

				StringBuilder sharedSB = new StringBuilder();

				for (ClassNode mixin : mixins) {
					try {
						mixinRemapper.remapClass(mixin);
						source.overrideClass(mixin);
					} catch (Exception e) {
						exception = e;
						return false;
					}
				}
				for (String modClass : mod.getClasses()) {
					ClassNode node = source.getClass(modClass);
					if (node == null) {
						Launch.LOGGER.logErr("Could not find mod class %s", modClass);
						continue;
					}
					nodeRemapper.remapNode(node, sharedSB);
					source.overrideClass(node);
				}
			}

			try {
				transformer.addMixin(source, mixinConfig);
			} catch (IllegalStateException e) {
				exception = e;
				return false;
			}
		}

		return true;
	}
}
