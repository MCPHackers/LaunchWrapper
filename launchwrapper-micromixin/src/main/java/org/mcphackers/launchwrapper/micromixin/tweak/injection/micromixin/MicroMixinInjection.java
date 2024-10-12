package org.mcphackers.launchwrapper.micromixin.tweak.injection.micromixin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.mcphackers.launchwrapper.Launch;
import org.mcphackers.launchwrapper.LaunchConfig;
import org.mcphackers.launchwrapper.micromixin.MappingSource;
import org.mcphackers.launchwrapper.micromixin.MixinMod;
import org.mcphackers.launchwrapper.micromixin.ModMetadata;
import org.mcphackers.launchwrapper.micromixin.transformer.AccessWidenerImpl;
import org.mcphackers.launchwrapper.micromixin.transformer.MappingProvider;
import org.mcphackers.launchwrapper.micromixin.transformer.Mappings;
import org.mcphackers.launchwrapper.micromixin.transformer.MemberLookup;
import org.mcphackers.launchwrapper.micromixin.transformer.NopMappingLookup;
import org.mcphackers.launchwrapper.micromixin.transformer.RemappedMemberLookup;
import org.mcphackers.launchwrapper.micromixin.tweak.MicroMixinTweak;
import org.mcphackers.launchwrapper.tweak.injection.Injection;
import org.mcphackers.launchwrapper.util.ClassNodeSource;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.stianloader.micromixin.remapper.MemberLister;
import org.stianloader.micromixin.remapper.MicromixinRemapper;
import org.stianloader.micromixin.transform.api.MixinConfig;
import org.stianloader.remapper.HierarchyAwareMappingDelegator;
import org.stianloader.remapper.HierarchyAwareMappingDelegator.TopLevelMemberLookup;
import org.stianloader.remapper.Remapper;

import net.fabricmc.accesswidener.AccessWidener;
import net.fabricmc.accesswidener.AccessWidenerClassVisitor;

public class MicroMixinInjection implements Injection {
    
    private Collection<MixinMod> mixinMods;
    private MicroMixinTweak mixinTweak;

    public Exception exception;
    
    public MicroMixinInjection(Collection<MixinMod> mixinMods, MicroMixinTweak mixinTweak) {
        this.mixinMods = mixinMods;
        this.mixinTweak = mixinTweak;
    }

    @Override
    public String name() {
        return "Micromixin";
    }


    @Override
    public boolean required() {
        return true;
    }

    @Override
    public boolean apply(ClassNodeSource source, LaunchConfig config) {
        for(MixinMod mod : mixinMods) {
            ModMetadata metadata = mod.getMetadata();
            Launch.LOGGER.log("Loading mod: %s %s", metadata.name, metadata.version);
            MixinConfig mixinConfig = mod.getMixinConfig();
            List<ClassNode> mixins = new ArrayList<>();
            for(String mixin : mixinConfig.mixins) {
                ClassNode mixinNode = source.getClass(mixinConfig.mixinPackage + "/" + mixin);
                if(mixinNode != null) {
                    mixins.add(mixinNode);
                } else {
                    Launch.LOGGER.log("WARNING: Missing mixin - " + mixinConfig.mixinPackage + "/" + mixin);
                }
            }

            MappingSource mappingSource = mod.getMappingSource();
            TopLevelMemberLookup memberLookup;
            MemberLister memberLister;
            Mappings mappings = mappingSource == null ? null : mappingSource.getMappings();
            HierarchyAwareMappingDelegator<?> mappingProvider;
            if(mappingSource != null) {
                RemappedMemberLookup remappedMemberLookup = new RemappedMemberLookup(source, mappings);
                memberLookup = remappedMemberLookup;
                memberLister = remappedMemberLookup;
                mappingProvider = new HierarchyAwareMappingDelegator<MappingProvider>(new MappingProvider(mappings), memberLookup);
            } else {
                MemberLookup classNodeMemberLookup = new MemberLookup(source);
                memberLookup = classNodeMemberLookup;
                memberLister = classNodeMemberLookup;
                mappingProvider = new HierarchyAwareMappingDelegator<NopMappingLookup>(new NopMappingLookup(), memberLookup);
            }
            
            AccessWidenerImpl awImpl = mod.getAccessWidener();
            if(awImpl != null) {
                AccessWidener aw = awImpl.getRemappedAccessWidener(mappingProvider);
                for(String target : aw.getTargets()) {
                    ClassNode targetNode = source.getClass(target.replace('.', '/'));
                    if(targetNode == null) {
                        continue;
                    }
                    ClassNode accessibleNode = new ClassNode();
                    ClassVisitor visitor = AccessWidenerClassVisitor.createClassVisitor(Opcodes.ASM9, accessibleNode, aw);
                    targetNode.accept(visitor);
                    source.overrideClass(accessibleNode);
                }
            }
            if(mappingSource != null) {
                MicromixinRemapper remapper = new MicromixinRemapper(mappingProvider,
                    mappingProvider, memberLister);
                
                Remapper remapper2 = new Remapper(mappingProvider);

                StringBuilder sharedSB = new StringBuilder();

                for(ClassNode mixin : mixins) {
                    try {
                        remapper.remapClass(mixin);
                        // remapper2.remapNode(mixin, sharedSB);
                        source.overrideClass(mixin);
                    } catch (Exception e) {
                        exception = e;
                        return false;
                    }
                }
                for(String modClass : mod.getClasses()) {
                    ClassNode node = source.getClass(modClass);
                    if(node == null) {
                        Launch.LOGGER.logErr("Could not find mod class %s", modClass);
                        continue;
                    }
                    remapper2.remapNode(node, sharedSB);
                    source.overrideClass(node);
                }
            }

            try {
                mixinTweak.transformer.addMixin(source, mixinConfig);
            } catch (IllegalStateException e) {
                exception = e;
                return false;
            }
        }

        return true;
    }
    
}
