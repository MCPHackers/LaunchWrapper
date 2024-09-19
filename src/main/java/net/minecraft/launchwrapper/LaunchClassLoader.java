package net.minecraft.launchwrapper;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.mcphackers.launchwrapper.tweak.LegacyLauncherTweak;
import org.mcphackers.launchwrapper.util.Util;

public class LaunchClassLoader extends URLClassLoader {

    private org.mcphackers.launchwrapper.loader.LaunchClassLoader classLoader;
    private LegacyLauncherTweak tweak;
    private List<URL> sources = new ArrayList<URL>();

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
        classLoader.addOverrideExclusion(toExclude);
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
        InputStream is = classLoader.getClassAsStream(name);
        if(is == null) {
            return null;
        }
        return Util.readStream(is);
    }
    
    public void clearNegativeEntries(Set<String> entriesToClear) {
    }
}
