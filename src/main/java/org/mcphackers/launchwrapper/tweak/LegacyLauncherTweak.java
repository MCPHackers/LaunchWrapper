package org.mcphackers.launchwrapper.tweak;

import static org.mcphackers.launchwrapper.Launch.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.mcphackers.launchwrapper.LaunchConfig;
import org.mcphackers.launchwrapper.loader.RawBytecodeProvider;
import org.mcphackers.launchwrapper.target.LaunchTarget;
import org.mcphackers.launchwrapper.target.MainLaunchTarget;
import org.mcphackers.launchwrapper.tweak.injection.Injection;
import org.mcphackers.launchwrapper.tweak.injection.forge.ForgeClassLoader;
import org.mcphackers.launchwrapper.tweak.injection.vanilla.ChangeBrand;
import org.mcphackers.launchwrapper.util.ClassNodeSource;

import net.minecraft.launchwrapper.IClassNameTransformer;
import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;

public class LegacyLauncherTweak extends Tweak {
    public IClassNameTransformer renameTransformer;
    protected List<IClassTransformer> transformers = new ArrayList<IClassTransformer>();
    protected List<String> arguments = new ArrayList<String>();
    protected Tweak baseTweak;
    ITweaker primaryTweaker;
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
        injections.addAll(baseTweak.getInjections());
        return injections;
    }

    @Override
    public List<LazyTweaker> getLazyTweakers() {
        List<LazyTweaker> tweakers = new ArrayList<LazyTweaker>();
        tweakers.addAll(baseTweak.getLazyTweakers());
        return tweakers;
    }

    @Override
    public LaunchTarget getLaunchTarget() {
        String target = primaryTweaker.getLaunchTarget();
        LOGGER.log("Launching wrapped minecraft {%s}", target);
        return new MainLaunchTarget(target, arguments.toArray(new String[arguments.size()]));
    }

    @Override
    public void prepare(org.mcphackers.launchwrapper.loader.LaunchClassLoader launchloader) {
        Launch.classLoader = new LaunchClassLoader(launchloader, this);
        List<String> tweakClassNames = new ArrayList<String>();
        tweakClassNames.add(config.tweakClass.get());
        Launch.blackboard.put("TweakClasses", tweakClassNames);
        Launch.blackboard.put("ArgumentList", new ArrayList<String>());
        Launch.minecraftHome = config.gameDir.get();
        Launch.assetsDir = config.assetsDir.get();
        new ForgeClassLoader().apply(launchloader, config);
        new ChangeBrand().apply(launchloader, config);
        launchloader.setBytecodeProvider(new LegacyLauncherBytecodeProvider(transformers));
        runTweakers();
        initialized = true;
    }

    private List<String> getArgs() {
        List<String> args = new ArrayList<String>();
        LaunchConfig configCopy = config.clone();
        configCopy.gameDir.set(null);
        configCopy.assetsDir.set(null);
        configCopy.version.set(null);
        for (Map.Entry<String, String> entry : configCopy.getArgsAsMap().entrySet()) {
            args.add("--" + entry.getKey());
            args.add(entry.getValue());
        }
        return args;
    }

    @SuppressWarnings("unchecked")
    protected void runTweakers() {
        List<String> args = getArgs();
        List<String> tweakClassNames = (List<String>)Launch.blackboard.get("TweakClasses");

        final Set<String> allTweakerNames = new HashSet<String>();
        final List<ITweaker> allTweakers = new ArrayList<ITweaker>();
        try {
            final List<ITweaker> tweakers = new ArrayList<ITweaker>(tweakClassNames.size() + 1);
            Launch.blackboard.put("Tweaks", tweakers);
            do {
                for (final Iterator<String> it = tweakClassNames.iterator(); it.hasNext();) {
                    final String tweakName = it.next();
                    if (allTweakerNames.contains(tweakName)) {
                        LOGGER.log("Tweak class name %s has already been visited -- skipping", tweakName);
                        it.remove();
                        continue;
                    } else {
                        allTweakerNames.add(tweakName);
                    }
                    LOGGER.log("Loading tweak class name %s", tweakName);

                    Launch.classLoader.addClassLoaderExclusion(tweakName.substring(0, tweakName.lastIndexOf('.')));
                    final ITweaker tweaker = (ITweaker) Class.forName(tweakName, true, getClass().getClassLoader())
                            .newInstance();
                    tweakers.add(tweaker);

                    it.remove();
                    if (primaryTweaker == null) {
                        LOGGER.log("Using primary tweak class name %s", tweakName);
                        primaryTweaker = tweaker;
                    }
                }

                for (final Iterator<ITweaker> it = tweakers.iterator(); it.hasNext();) {
                    final ITweaker tweaker = it.next();
                    LOGGER.log("Calling tweak class %s", tweaker.getClass().getName());
                    tweaker.acceptOptions(args, config.gameDir.get(), config.assetsDir.get(), config.version.get());
                    tweaker.injectIntoClassLoader(Launch.classLoader);
                    allTweakers.add(tweaker);
                    it.remove();
                }
            } while (!tweakClassNames.isEmpty());
            for (final ITweaker tweaker : allTweakers) {
                arguments.addAll(Arrays.asList(tweaker.getLaunchArguments()));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<IClassTransformer> getTransformers() {
        return Collections.unmodifiableList(transformers);
    }

    public void registerTransformer(String transformerClassName) {
        try {
            IClassTransformer transformer = (IClassTransformer) Launch.classLoader.loadClass(transformerClassName)
                    .newInstance();
            transformers.add(transformer);
            if (transformer instanceof IClassNameTransformer && renameTransformer == null) {
                renameTransformer = (IClassNameTransformer) transformer;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public class LegacyLauncherBytecodeProvider implements RawBytecodeProvider {
        List<IClassTransformer> transformers;

        public LegacyLauncherBytecodeProvider(List<IClassTransformer> transformers) {
            this.transformers = transformers;
        }

        public byte[] getClassBytecode(String name) {
            String untransformedName = renameTransformer == null ? name : renameTransformer.unmapClassName(name);
            String transformedName = renameTransformer == null ? name : renameTransformer.remapClassName(name);
            try {
                byte[] transformed = Launch.classLoader.getClassBytes(untransformedName);
                for (IClassTransformer transformer : transformers) {
                    transformed = transformer.transform(untransformedName, transformedName, transformed);
                }
                return transformed;
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        public String getClassName(String name) {
            return renameTransformer == null ? name : renameTransformer.remapClassName(name);
        }
    }

}
