package org.mcphackers.launchwrapper.tweak;

import static org.mcphackers.launchwrapper.Launch.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.mcphackers.launchwrapper.LaunchConfig;
import org.mcphackers.launchwrapper.target.LaunchTarget;
import org.mcphackers.launchwrapper.target.MainLaunchTarget;
import org.mcphackers.launchwrapper.tweak.injection.Injection;
import org.mcphackers.launchwrapper.util.ClassNodeSource;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

import net.minecraft.launchwrapper.IClassNameTransformer;
import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;

public class LegacyLauncherTweak extends Tweak {
    public IClassNameTransformer renameTransformer;
    protected List<IClassTransformer> transformers = new ArrayList<IClassTransformer>();
    ITweaker primaryTweaker;
    LaunchClassLoader loader;
    Tweak baseTweak;
    boolean initialized = false;

    public LegacyLauncherTweak(LaunchConfig config, Tweak baseTweak) {
        super(config);
        this.baseTweak = baseTweak;
    }

    private class InjectTest implements Injection {

        @Override
        public String name() {
            return "LegacyLauncher compatibility";
        }

        @Override
        public boolean required() {
            return true;
        }

        @Override
        public boolean apply(ClassNodeSource source, LaunchConfig config) {
            return initialized;
        }

    }

    @Override
    public List<Injection> getInjections() {
        List<Injection> injections = new ArrayList<Injection>();
        injections.add(new InjectTest());
        // FIXME breaks forge patches?
        injections.addAll(baseTweak.getInjections());
        return injections;
    }

    @Override
    public List<LazyTweaker> getLazyTweakers() {
        List<LazyTweaker> tweakers = new ArrayList<LazyTweaker>();
        tweakers.add(new LegacyLauncherLazyTweaker(transformers));
        tweakers.addAll(baseTweak.getLazyTweakers());
        return tweakers;
    }

    @Override
    public LaunchTarget getLaunchTarget() {
        return new MainLaunchTarget(primaryTweaker.getLaunchTarget(), primaryTweaker.getLaunchArguments());
    }

    @Override
    public void prepare(org.mcphackers.launchwrapper.loader.LaunchClassLoader launchloader) {
        loader = new LaunchClassLoader(launchloader, this);
        List<String> tweakClassNames = new ArrayList<String>();
        tweakClassNames.add(config.tweakClass.get());
        Launch.blackboard.put("TweakClasses", tweakClassNames);
        Launch.blackboard.put("ArgumentList", new ArrayList<String>());
        Launch.minecraftHome = config.gameDir.get();
        Launch.assetsDir = config.assetsDir.get();
        List<String> args = new ArrayList<String>();
        // FIXME Remove gamedir, assetsdir and version in a more clean way
        args.addAll(Arrays.asList(config.getArgs()));
        for(int i = 0; i < args.size(); i++) {
            if(i + 1 < args.size()) {
                while(args.get(i).equals("--version")
                || args.get(i).equals("--gameDir")
                || args.get(i).equals("--assetsDir")) {
                    args.remove(i+1);
                    args.remove(i);
                }
            }
        }
        try {
            final Set<String> allTweakerNames = new HashSet<String>();
            final List<ITweaker> tweakers = new ArrayList<ITweaker>(tweakClassNames.size() + 1);
            Launch.blackboard.put("Tweaks", tweakers);
            do {
                for (final Iterator<String> it = tweakClassNames.iterator(); it.hasNext(); ) {
                    final String tweakName = it.next();
                    if (allTweakerNames.contains(tweakName)) {
                        LOGGER.log("Tweak class name %s has already been visited -- skipping", tweakName);
                        it.remove();
                        continue;
                    } else {
                        allTweakerNames.add(tweakName);
                    }
                    LOGGER.log("Loading tweak class name %s", tweakName);

                    loader.addClassLoaderExclusion(tweakName.substring(0,tweakName.lastIndexOf('.')));
                    final ITweaker tweaker = (ITweaker) Class.forName(tweakName, true, loader).newInstance();
                    tweakers.add(tweaker);

                    it.remove();
                    if (primaryTweaker == null) {
                        LOGGER.log("Using primary tweak class name %s", tweakName);
                        primaryTweaker = tweaker;
                    }
                }

                for (final Iterator<ITweaker> it = tweakers.iterator(); it.hasNext(); ) {
                    final ITweaker tweaker = it.next();
                    LOGGER.log("Calling tweak class %s", tweaker.getClass().getName());
                    tweaker.acceptOptions(args, config.gameDir.get(), config.assetsDir.get(), config.version.get());
                    tweaker.injectIntoClassLoader(loader);
                    it.remove();
                }
            } while (!tweakClassNames.isEmpty());
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        initialized = true;
    }


    public List<IClassTransformer> getTransformers() {
        return Collections.unmodifiableList(transformers);
    }

    public void registerTransformer(String transformerClassName) {
        try {
            IClassTransformer transformer = (IClassTransformer) loader.loadClass(transformerClassName).newInstance();
            transformers.add(transformer);
            if (transformer instanceof IClassNameTransformer && renameTransformer == null) {
                renameTransformer = (IClassNameTransformer) transformer;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public class LegacyLauncherLazyTweaker implements LazyTweaker {
        List<IClassTransformer> transformers;
        public LegacyLauncherLazyTweaker(List<IClassTransformer> transformers) {
            this.transformers = transformers;
        }

        public boolean tweakClass(ClassNodeSource source, String name) {
            name = name.replace("/", ".");
            String untransformedName = renameTransformer == null ? name : renameTransformer.unmapClassName(name);
            String transformedName = renameTransformer == null ? name : renameTransformer.remapClassName(name);
            // Mojang LW caches byte[] arrays. MCPHackers LW caches ClassNodes.
            // That's why there's such a messy convertion back and forth
            byte[] transformed = getClass(untransformedName);
            for(IClassTransformer transformer : transformers) {
                transformed = transformer.transform(untransformedName, transformedName, transformed);
            }
            if(transformed == null) {
                return false;
            }
            ClassReader reader = new ClassReader(transformed);
            ClassNode node = new ClassNode();
            reader.accept(node, ClassReader.SKIP_FRAMES);
            source.overrideClass(node);
            return true;
        }

        private byte[] getClass(String name) {
            try {
                return loader.getClassBytes(name);
            } catch (IOException e) {
                return null;
            }
        }
    }

}
