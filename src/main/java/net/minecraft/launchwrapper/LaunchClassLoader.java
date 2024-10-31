package net.minecraft.launchwrapper;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.Manifest;

@Deprecated
@SuppressWarnings("unused")
public class LaunchClassLoader extends org.mcphackers.launchwrapper.loader.LaunchClassLoader {

	private final List<URL> sources = new ArrayList<URL>();

	private final Map<String, Class<?>> cachedClasses = Collections.<String, Class<?>>emptyMap();
	private final Set<String> invalidClasses = new HashSet<String>();
	private final Set<String> classLoaderExceptions;
	private final Set<String> transformerExceptions = new HashSet<String>();
	private final List<IClassTransformer> transformers = new ArrayList<IClassTransformer>();
	public IClassNameTransformer renameTransformer;

	// Avoids exceptions with mods like FoamFix and CensoredASM
	@Deprecated
	private static final Manifest EMPTY = new Manifest();
	@Deprecated
	private Map<Package, Manifest> packageManifests = null;

	public LaunchClassLoader(ClassLoader parent) {
		super(parent);
		// Some mods refer to these fields via reflection
		classLoaderExceptions = exclusions;
	}

	public void registerTransformer(String transformerClassName) {
		try {
			IClassTransformer transformer = (IClassTransformer)Launch.classLoader.loadClass(transformerClassName).newInstance();
			transformers.add(transformer);
			if (transformer instanceof IClassNameTransformer && renameTransformer == null) {
				renameTransformer = (IClassNameTransformer)transformer;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public List<IClassTransformer> getTransformers() {
		return Collections.unmodifiableList(transformers);
	}

	public void addClassLoaderExclusion(String pkg) {
		addExclusion(pkg);
	}

	public void addTransformerExclusion(String pkg) {
		transformerExceptions.add(pkg + (pkg.endsWith(".") ? "" : "."));
	}

	private String untransformName(final String name) {
		if (renameTransformer != null) {
			return renameTransformer.unmapClassName(name);
		}
		return name;
	}

	private String transformName(final String name) {
		if (renameTransformer != null) {
			return renameTransformer.remapClassName(name);
		}
		return name;
	}

	/**
	 * Foundation API extension
	 * used by Cleanroom
	 *
	 * @param name class name
	 * @return
	 */
	public boolean isClassLoaded(String name) {
		return getLoadedClass(name) != null;
	}

	/**
	 * TODO Foundation API extension
	 * used by Cleanroom
	 *
	 * @param transformerClassName
	 */
	public void registerSuperTransformer(String transformerClassName) {
		registerTransformer(transformerClassName);
	}

	@Override
	public void addURL(URL url) {
		super.addURL(url);
		sources.add(url);
	}

	public List<URL> getSources() {
		return sources;
	}

	public void clearNegativeEntries(Set<String> entriesToClear) {
		invalidClasses.clear();
	}

	/**
	 * Called by Cleanroom loader
	 *
	 * @param name
	 * @param transformedName
	 * @param data
	 * @return
	 */
	public byte[] runTrasformers(String name, String transformedName, byte[] data) {
		byte[] transformed = data;
		for (IClassTransformer transformer : transformers) {
			transformed = transformer.transform(name, transformedName, transformed);
		}
		return transformed;
	}

	@Override
	public byte[] getTransformedClass(String name) throws IOException {
		String untransformedName = untransformName(name);
		String transformedName = transformName(name);
		byte[] classData = getClassBytes(untransformedName);
		for (final String exception : transformerExceptions) {
			if (name.startsWith(exception)) {
				return classData;
			}
		}
		return runTrasformers(untransformedName, transformedName, classData);
	}

	@Override
	public String getTransformedName(String name) {
		return transformName(name);
	}

	public static LaunchClassLoader instantiate() {
		LaunchClassLoader loader = new LaunchClassLoader(LaunchClassLoader.class.getClassLoader());
		Thread.currentThread().setContextClassLoader(loader);
		return loader;
	}
}
