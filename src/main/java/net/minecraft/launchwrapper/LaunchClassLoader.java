package net.minecraft.launchwrapper;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.mcphackers.launchwrapper.tweak.LegacyLauncherTweak;
import org.mcphackers.launchwrapper.util.UnsafeUtils;

@SuppressWarnings("unused")
public class LaunchClassLoader extends URLClassLoader {

    private org.mcphackers.launchwrapper.loader.LaunchClassLoader classLoader;
    private LegacyLauncherTweak tweak;
    private List<URL> sources = new ArrayList<URL>();

    private final Map<String, Class<?>> cachedClasses = Collections.<String, Class<?>>emptyMap();
    private final Set<String> invalidClasses = new HashSet<String>();
    // private final Set<String> classLoaderExceptions;
    // private final Set<String> transformerExceptions;

    public LaunchClassLoader(org.mcphackers.launchwrapper.loader.LaunchClassLoader classLoader, LegacyLauncherTweak tweak) {
        super(new URL[0]);
        this.classLoader = classLoader;
        this.tweak = tweak;
    }

    public void registerTransformer(String transformerClassName) {
        tweak.registerTransformer(transformerClassName);
    }

    public List<IClassTransformer> getTransformers() {
        return tweak.getTransformers();
    }

    public void addClassLoaderExclusion(String toExclude) {
        classLoader.addExclusion(toExclude);
    }

    public void addTransformerExclusion(String toExclude) {
        classLoader.addTransformExclusion(toExclude);
    }

    private String untransformName(final String name) {
        if (tweak.renameTransformer != null) {
            return tweak.renameTransformer.unmapClassName(name);
        }

        return name;
    }

    private String transformName(final String name) {
        if (tweak.renameTransformer != null) {
            return tweak.renameTransformer.remapClassName(name);
        }

        return name;
    }

    @Override
    public Class<?> findClass(String name) throws ClassNotFoundException {
        return classLoader.findClass(name);
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        return classLoader.loadClass(name);
    }

    public void addURL(URL url) {
        classLoader.addURL(url);
        sources.add(url);
    }

    public List<URL> getSources() {
        return sources;
    }

    public byte[] getClassBytes(String name) throws IOException {
        return classLoader.getClassBytes(name);
    }
    
    public void clearNegativeEntries(Set<String> entriesToClear) {
        invalidClasses.clear();
    }
}
