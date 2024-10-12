package org.mcphackers.launchwrapper.micromixin.tweak;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.mcphackers.launchwrapper.LaunchConfig;
import org.mcphackers.launchwrapper.loader.LaunchClassLoader;
import org.mcphackers.launchwrapper.micromixin.FileMixinMod;
import org.mcphackers.launchwrapper.micromixin.MappingSource;
import org.mcphackers.launchwrapper.micromixin.MixinMod;
import org.mcphackers.launchwrapper.micromixin.tweak.injection.micromixin.MicroMixinInjection;
import org.mcphackers.launchwrapper.micromixin.tweak.injection.micromixin.PackageAccessFixer;
import org.mcphackers.launchwrapper.target.LaunchTarget;
import org.mcphackers.launchwrapper.tweak.LazyTweaker;
import org.mcphackers.launchwrapper.tweak.Tweak;
import org.mcphackers.launchwrapper.tweak.injection.Injection;
import org.mcphackers.launchwrapper.util.ClassNodeSource;
import org.mcphackers.launchwrapper.util.Util;
import org.objectweb.asm.tree.ClassNode;
import org.stianloader.micromixin.transform.api.MixinTransformer;
import org.stianloader.micromixin.transform.api.supertypes.ASMClassWrapperProvider;
import org.stianloader.micromixin.transform.api.supertypes.ClassWrapperPool;

public class MicroMixinTweak extends Tweak {
    private static final boolean ACCESS_FIXER = Boolean.parseBoolean(System.getProperty("launchwrapper.accessfixer", "false"));
    public static final boolean DEV = Boolean.parseBoolean(System.getProperty("launchwrapper.dev", "false"));
    
    protected List<MixinMod> mods = new ArrayList<MixinMod>();
    protected Tweak baseTweak;
    protected MicroMixinInjection microMixinInjection = new MicroMixinInjection(mods, this);

    public MicroMixinTweak(LaunchConfig launch, Tweak tweak) {
        super(launch);
        baseTweak = tweak;
    }

    public final MixinTransformer<ClassNodeSource> transformer = new MixinTransformer<>((source, name) -> source.getClass(name), getPool());

    public void prepare(LaunchClassLoader loader) {
        File modDir = new File(config.gameDir.get(), "mods");
        URL url = loader.getResource("launchwrapper.mod.json");
        if(url != null) {
            try {
                File f = Util.getSource(url, "launchwrapper.mod.json");
                MixinMod mod = new FileMixinMod(f);
                mods.add(mod);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if(modDir.isDirectory()) {
            for(File f : modDir.listFiles()) {
                try {
                    if(f.getName().endsWith(".jar")) {
                        MixinMod mod = new FileMixinMod(f);
                        mods.add(mod);
                        loader.addURL(f.toURI().toURL());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * @return List of mixin mods, which themselves contain mixin classes
     */
    public List<MixinMod> getMixins() {
        return mods;
    }

    private ClassWrapperPool getPool() {
        return new ClassWrapperPool(Collections.singletonList(new ASMClassWrapperProvider() {
            public ClassNode getNode(@NotNull String name) {
                return source.getClass(name);
            }
        }));
    }

    @Override
    public List<Injection> getInjections() {
        List<MixinMod> mods = getMixins();
        List<Injection> injects = new ArrayList<Injection>();
        for(MixinMod mod : mods) {
            MappingSource mappingSource = mod.getMappingSource();
            if(mappingSource != null) {
                injects.add(mappingSource);
            }
        }
        injects.addAll(baseTweak.getInjections());
        injects.add(microMixinInjection);
        return injects;
    }

    public List<LazyTweaker> getLazyTweakers() {
        List<LazyTweaker> list = new ArrayList<>();
        list.addAll(baseTweak.getLazyTweakers());
        if(ACCESS_FIXER) {
            list.add(new PackageAccessFixer());
        }
        list.add(new MicroMixinLoaderTweak(transformer));
        return list;
    }

    public boolean handleError(LaunchClassLoader loader, Throwable t) {
        return baseTweak.handleError(loader, microMixinInjection.exception == null ? t : microMixinInjection.exception);
    }

    @Override
    public LaunchTarget getLaunchTarget() {
        return baseTweak.getLaunchTarget();
    }
    
}
