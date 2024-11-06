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
	private final Map<String, byte[]> resourceCache = Collections.<String, byte[]>emptyMap();
	private final Set<String> invalidClasses = new HashSet<String>();
	private final Set<String> classLoaderExceptions;
	private final Set<String> transformerExceptions = new HashSet<String>();
	private final List<IClassTransformer> transformers = new ArrayList<IClassTransformer>();
	private IClassNameTransformer renameTransformer;

	// Avoids exceptions with mods like FoamFix and CensoredASM
	private static final Manifest EMPTY = new Manifest();
	private Map<Package, Manifest> packageManifests = null;

	public LaunchClassLoader(ClassLoader parent) {
		this(getInitialURLs(parent), parent);
	}
	public LaunchClassLoader(URL[] sources) {
		this(sources, LaunchClassLoader.class.getClassLoader());
	}
	public LaunchClassLoader(URL[] sources, ClassLoader parent) {
		super(sources, parent);
		classLoaderExceptions = exclusions;
		addClassLoaderExclusion("org.lwjgl.");
		addClassLoaderExclusion("org.apache.logging.");
		addClassLoaderExclusion("net.minecraft.launchwrapper.");

		addTransformerExclusion("javax.");
		addTransformerExclusion("argo.");
		addTransformerExclusion("org.objectweb.asm.");
		addTransformerExclusion("com.google.common.");
		addTransformerExclusion("org.bouncycastle.");
		addTransformerExclusion("net.minecraft.launchwrapper.injector.");
	}

	public void registerTransformer(String transformerClassName) {
		try {
			IClassTransformer transformer = (IClassTransformer)loadClass(transformerClassName).newInstance();
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

	public void addClassLoaderExclusion(String prefix) {
		addExclusion(prefix);
	}

	public void addTransformerExclusion(String prefix) {
		transformerExceptions.add(prefix);
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

	private byte[] runTrasformers(String name, String transformedName, byte[] data) {
		byte[] transformed = data;
		for (IClassTransformer transformer : transformers) {
			transformed = transformer.transform(name, transformedName, transformed);
		}
		return transformed;
	}

	@Override
	protected byte[] getTransformedClass(String name) throws IOException {
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
	protected String getTransformedName(String name) {
		return transformName(name);
	}

	public static LaunchClassLoader instantiate() {
		LaunchClassLoader loader = new LaunchClassLoader(LaunchClassLoader.class.getClassLoader());
		Thread.currentThread().setContextClassLoader(loader);
		return loader;
	}
}
