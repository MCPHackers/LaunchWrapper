package org.mcphackers.launchwrapper.loader;

import static org.objectweb.asm.ClassWriter.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.security.CodeSigner;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.mcphackers.launchwrapper.Launch;
import org.mcphackers.launchwrapper.tweak.Tweaker;
import org.mcphackers.launchwrapper.util.ClassNodeSource;
import org.mcphackers.launchwrapper.util.ResourceSource;
import org.mcphackers.launchwrapper.util.Util;
import org.mcphackers.launchwrapper.util.asm.NodeHelper;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

// URLClassLoader is required to support ModLoader loading mods from mod folder, as well as Forge

/**
 * The main class loader for overwritten and patched classes
 */
public class LaunchClassLoader extends URLClassLoader implements ClassNodeSource, ResourceSource {

	public static final int CLASS_VERSION = getSupportedClassVersion();

	protected ClassLoader parent;
	protected HighPriorityClassLoader urlClassLoader;
	protected List<Tweaker> tweaks;
	protected Set<String> exclusions = new HashSet<String>();
	protected Set<String> transformerExclusions = new HashSet<String>();
	/**
	 * Keys should contain dots
	 */
	protected Map<String, ClassNode> overridenClasses = new HashMap<String, ClassNode>();
	protected Map<String, byte[]> overridenResources = new HashMap<String, byte[]>();
	protected Map<String, String> overridenSource = new HashMap<String, String>();
	/**
	 * Keys should contain slashes
	 */
	protected Map<String, ClassNode> classNodeCache = new HashMap<String, ClassNode>();

	private File debugOutput;
	protected boolean useDummyCert = Boolean.parseBoolean(System.getProperty("launchwrapper.useDummyCertificate", "true"));

	protected static class HighPriorityClassLoader extends URLClassLoader {
		public HighPriorityClassLoader(URL[] urls) {
			super(urls);
		}

		@Override
		public void addURL(URL url) {
			super.addURL(url);
		}
	}

	public LaunchClassLoader(ClassLoader parent) {
		this(getInitialURLs(parent), parent);
	}

	protected LaunchClassLoader(URL[] sources, ClassLoader parent) {
		super(sources, getParentClassLoader());
		this.parent = parent;
		this.urlClassLoader = new HighPriorityClassLoader(new URL[0]);
		// Exclude references to libraries loaded in parent CL
		addExclusion("java.");
		addExclusion("javax.");
		addExclusion("sun.");
		addExclusion("com.sun.");
		addExclusion("org.mcphackers.launchwrapper.");
		addExclusion("org.objectweb.asm.");
		addExclusion("org.json.");
	}

	private static ClassLoader getParentClassLoader() {
		try {
			return (ClassLoader)ClassLoader.class.getMethod("getPlatformClassLoader").invoke(null); // Java 9+ only
		} catch (NoSuchMethodException e) {
			return null; // fall back to boot cl
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	protected static URL[] getInitialURLs(ClassLoader parent) {
		List<URL> urls = new ArrayList<URL>();
		if (parent instanceof URLClassLoader) {
			return ((URLClassLoader)parent).getURLs();
		}
		String classpath = System.getProperty("java.class.path");
		String[] classpathSplit = classpath.split(File.pathSeparator);

		for (String singlePath : classpathSplit) {
			try {
				urls.add(new File(singlePath).toURI().toURL());
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}
		return urls.toArray(new URL[urls.size()]);
	}

	public void setDebugOutput(File directory) {
		debugOutput = directory;
	}

	public void addMod(URL url) {
		classNodeCache.clear();
		urlClassLoader.addURL(url);
	}

	@Override
	public void addURL(URL url) {
		super.addURL(url);
	}

	@Override
	public URL findResource(String resource) {
		URL url = getOverridenResourceURL(resource);
		if (url != null) {
			return url;
		}
		return getOriginalURL(resource);
	}

	URL getOriginalURL(String resource) {
		URL url = urlClassLoader.findResource(resource);
		if (url != null) {
			return url;
		}
		url = parent.getResource(resource);
		return url == null ? super.findResource(resource) : url;
	}

	private URL getOverridenResourceURL(String resource) {
		try {
			if (overridenResources.get(resource) != null || overridenClasses.get(classNameFromResource(resource)) != null) {
				URL originalUrl = getOriginalURL(resource);
				if (originalUrl == null) {
					return new URL("file", "", -1, resource, new ClassLoaderURLHandler(this));
				}
				return new URL(
					originalUrl.getProtocol(),
					originalUrl.getHost(),
					originalUrl.getPort(),
					resource,
					new ClassLoaderURLHandler(this));
			}
		} catch (MalformedURLException ignored) {
		}
		return null;
	}

	@Override
	public Enumeration<URL> findResources(String name) throws IOException {
		final Enumeration<URL> resources = urlClassLoader.getResources(name);

		if (!resources.hasMoreElements()) {
			return parent.getResources(name);
		}

		return resources;
	}

	public void addExclusion(String prefix) {
		if (prefix == null) {
			return;
		}
		exclusions.add(prefix);
	}

	public void overrideClassSource(String name, String f) {
		if (f == null) {
			return;
		}
		overridenSource.put(className(name), f);
	}

	public void overrideResource(String name, byte[] data) {
		overridenResources.put(name, data);
	}

	public byte[] getResourceData(String path) {
		try {
			InputStream is = this.getResourceAsStream(path);
			if (is == null) {
				return null;
			}
			return Util.readStream(is);
		} catch (IOException e) {
			return null;
		}
	}

	@Override
	public Class<?> findClass(String name) throws ClassNotFoundException {
		assert name.contains(".");

		// Force wrap inject package
		if (overridenSource.containsKey(name) || name.startsWith("org.mcphackers.launchwrapper.inject.")) {
			return redefineClass(name);
		}

		for (String prefix : exclusions) {
			if (name.startsWith(prefix)) {
				return parent.loadClass(name);
			}
		}

		for (String prefix : transformerExclusions) {
			if (name.startsWith(prefix)) {
				return super.findClass(name);
			}
		}
		return redefineClass(name);
	}

	public void invokeMain(String launchTarget, String... args) {
		try {
			Thread.currentThread().setContextClassLoader(this);
			Class<?> mainClass = loadClass(launchTarget);
			mainClass.getDeclaredMethod("main", String[].class).invoke(null, (Object)args);
		} catch (InvocationTargetException e1) {
			if (e1.getCause() != null) {
				e1.getCause().printStackTrace();
			} else {
				e1.printStackTrace();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void overrideClass(ClassNode node) {
		if (node == null) {
			return;
		}
		overridenClasses.put(className(node.name), node);
		classNodeCache.put(node.name, node);
	}

	public void saveDebugDump(ClassNode node) {
		if (debugOutput == null) {
			return;
		}
		try {
			File cls = new File(debugOutput, node.name + ".dump");
			Class<?> traceVisitorClass = Class.forName("org.objectweb.asm.util.TraceClassVisitor");
			ClassVisitor trace = traceVisitorClass.asSubclass(ClassVisitor.class)
									 .getConstructor(java.io.PrintWriter.class)
									 .newInstance(new java.io.PrintWriter(cls));
			cls.getParentFile().mkdirs();
			node.accept(trace);
		} catch (ClassNotFoundException e) {
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void saveDebugClass(ClassNode node) {
		if (debugOutput == null) {
			return;
		}
		try {
			File cls = new File(debugOutput, node.name + ".class");
			cls.getParentFile().mkdirs();
			ClassWriter writer = new SafeClassWriter(this, COMPUTE_MAXS);
			node.accept(writer);
			byte[] classData = writer.toByteArray();
			FileOutputStream fos = new FileOutputStream(cls);
			try {
				fos.write(classData);
			} finally {
				fos.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * @param name name with slash separator
	 *
	 * @return parsed ClassNode
	 */
	public ClassNode getClass(String name) {
		assert !name.contains(".");
		ClassNode node = classNodeCache.get(name);
		if (node != null) {
			return node;
		}
		try {
			return NodeHelper.readClass(getTransformedClass(name), ClassReader.SKIP_FRAMES);
		} catch (IOException e) {
			return null;
		}
	}

	protected Class<?> redefineClass(String name) throws ClassNotFoundException {
		assert name.contains(".");
		String nodeName = classNodeName(name);
		if (tweaks != null) {
			boolean modified = false;
			for (Tweaker tweak : tweaks) {
				modified |= tweak.tweakClass(this, nodeName);
			}
			if (modified) {
				overrideClass(getClass(nodeName));
				// Silence it for now because it gets very spammy with no real information (especially in the case of widening package access in fabric env)
				// Launch.LOGGER.logDebug("Applying lazy tweak to %s", nodeName);
			}
		}
		ClassNode overridenNode = overridenClasses.get(name);
		if (overridenNode != null) {
			return redefineClass(overridenNode);
		}
		String transformedName = getTransformedName(name);
		try {
			Class<?> loadedClass = findLoadedClass(transformedName);
			if (loadedClass != null) {
				Launch.LOGGER.logDebug("Attempted to reference unremapped class: %s", name);
				return loadedClass;
			}
			byte[] data = getTransformedClass(transformedName);
			if (data == null) {
				throw new ClassNotFoundException(transformedName);
			}
			return defineClass(transformedName, data, getProtectionDomain(name));
		} catch (IOException e) {
			throw new ClassNotFoundException(transformedName, e);
		}
	}

	protected String getTransformedName(String name) {
		return name;
	}

	protected byte[] getTransformedClass(String name) throws IOException {
		return getClassBytes(name);
	}

	private Class<?> redefineClass(ClassNode node) {
		if (node == null) {
			return null;
		}
		saveDebugDump(node);
		saveDebugClass(node);
		ClassWriter writer = new SafeClassWriter(this, COMPUTE_MAXS | COMPUTE_FRAMES);
		node.accept(writer);
		classNodeCache.remove(node.name);
		return defineClass(className(node.name), writer.toByteArray());
	}

	public Class<?> getLoadedClass(String name) {
		return findLoadedClass(name);
	}

	public byte[] getClassBytes(String name) throws IOException {
		String className = classResourceName(name);
		InputStream is = this.getResourceAsStream(className);
		if (is == null) {
			is = parent.getResourceAsStream(className);
		}
		if (is == null) {
			return null;
		}
		return Util.readStream(is);
	}

	public void setLoaderTweakers(List<Tweaker> classLoaderTweaks) {
		tweaks = classLoaderTweaks;
	}

	private Class<?> defineClass(String name, byte[] classData) {
		return defineClass(name, classData, getProtectionDomain(name));
	}

	private Class<?> defineClass(String name, byte[] classData, ProtectionDomain protectionDomain) {
		int i = name.lastIndexOf('.');
		if (i != -1) {
			String pkgName = name.substring(0, i);
			if (getPackage(pkgName) == null) {
				definePackage(pkgName, null, null, null, null, null, null, null);
			}
		}
		return defineClass(name, classData, 0, classData.length, protectionDomain);
	}

	protected ProtectionDomain getProtectionDomain(String name) {
		URL resource = getOriginalURL(classResourceName(name));
		if (resource == null) {
			return null;
		}

		CodeSigner[] signers = null;
		int i = name.lastIndexOf('.');

		try {
			URLConnection urlConnection = resource.openConnection();
			if (urlConnection instanceof JarURLConnection) {
				final JarURLConnection jarURLConnection = (JarURLConnection)urlConnection;
				JarFile jarFile;
				jarFile = jarURLConnection.getJarFile();

				if (jarFile != null && jarFile.getManifest() != null) {
					final Manifest manifest = jarFile.getManifest();
					final JarEntry entry = jarFile.getJarEntry(classResourceName(name));
					if (entry != null) {
						signers = entry.getCodeSigners();
					}
					if (i != -1) {
						String pkgName = name.substring(0, i);

						Package pkg = getPackage(pkgName);
						if (pkg == null) {
							pkg = definePackage(pkgName, manifest, jarURLConnection.getJarFileURL());
						}
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		URL newResource = resource;

		CodeSource codeSource;
		// ModLoader fix
		if (resource.getProtocol().equals("jar")) {
			String path = resource.getPath();
			if (path.startsWith("file:")) {
				path = path.substring("file:".length());
				int j = path.lastIndexOf('!');
				if (j != -1) {
					path = path.substring(0, j);
				}
			}
			if (overridenSource.containsKey(name)) {
				path = overridenSource.get(name);
			}
			try {
				newResource = new URL("file", "", path);
			} catch (MalformedURLException e) {
			}
		}
		codeSource = new CodeSource(newResource, signers);
		return new ProtectionDomain(codeSource, null);
	}

	private static ClassNode getSystemClass(Class<?> cls) {
		InputStream is = ClassLoader.getSystemResourceAsStream(classResourceName(cls.getName()));
		if (is == null) {
			return null;
		}
		try {
			return NodeHelper.readClass(is, ClassReader.SKIP_FRAMES);
		} catch (IOException e) {
			Util.closeSilently(is);
			return null;
		}
	}

	private static int getSupportedClassVersion() {
		ClassNode objectClass = getSystemClass(Object.class);
		assert objectClass != null;
		return objectClass.version;
	}

	public static String className(String nameWithSlashes) {
		return nameWithSlashes.replace('/', '.');
	}

	public static String classNodeName(String nameWithDots) {
		return nameWithDots.replace('.', '/');
	}

	public static String classResourceName(String name) {
		return classNodeName(name) + ".class";
	}

	public static String classNameFromResource(String resource) {
		if (resource.endsWith(".class")) {
			return resource.substring(0, resource.length() - 6).replace('/', '.');
		}
		return null;
	}

	public static LaunchClassLoader instantiate() {
		LaunchClassLoader loader = new LaunchClassLoader(LaunchClassLoader.class.getClassLoader());
		return loader;
	}
}
