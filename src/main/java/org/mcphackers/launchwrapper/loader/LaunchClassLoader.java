package org.mcphackers.launchwrapper.loader;

import static org.objectweb.asm.ClassWriter.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.mcphackers.launchwrapper.Launch;
import org.mcphackers.launchwrapper.tweak.LazyTweaker;
import org.mcphackers.launchwrapper.util.ClassNodeSource;
import org.mcphackers.launchwrapper.util.ResourceSource;
import org.mcphackers.launchwrapper.util.Util;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

// URLClassLoader is required to support ModLoader loading mods from mod folder, as well as Forge

/**
 * The main class loader for overwritten and patched classes
 */
public class LaunchClassLoader extends URLClassLoader implements ClassNodeSource, ResourceSource {

	public static final int CLASS_VERSION = getSupportedClassVersion();
	private static LaunchClassLoader INSTANCE;

	private ClassLoader parent;
	private RawBytecodeProvider bytecodeProvider;
	private List<LazyTweaker> tweaks;
	private Set<String> exclusions = new HashSet<String>();
	private Set<String> transformExclusions = new HashSet<String>();
	/** Keys should contain dots */
	Map<String, ClassNode> overridenClasses = new HashMap<String, ClassNode>();
	Map<String, byte[]> overridenResources = new HashMap<String, byte[]>();
	Map<String, String> overridenSource = new HashMap<String, String>();
	/** Keys should contain slashes */
	private Map<String, ClassNode> classNodeCache = new HashMap<String, ClassNode>();
	private File debugOutput;

	public LaunchClassLoader(ClassLoader parent) {
		super(getInitialURLs(parent), null);
		this.parent = parent;
		addExclusion("java");
		addExclusion("javax");
		addExclusion("sun");
		addExclusion("net.minecraft.launchwrapper");
		addExclusion("org.mcphackers.launchwrapper");
		addExclusion("org.objectweb.asm");
		addExclusion("org.json");
	}

	private static URL[] getInitialURLs(ClassLoader parent) {
		if(parent instanceof URLClassLoader) {
			return ((URLClassLoader)parent).getURLs();
		}
		List<URL> urls = new ArrayList<URL>();
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

	public void setBytecodeProvider(RawBytecodeProvider provider) {
		bytecodeProvider = provider;
		classNodeCache.clear();
	}

    public void addURL(URL url) {
    	super.addURL(url);
    }

	public URL findResource(String name) {
		URL url = getOverridenResourceURL(name);
		if(url != null) {
			return url;
		}
		url = parent.getResource(name);
		return url == null ? super.findResource(name) : url;
	}

	public URL getResource(String name) {
		URL url = getOverridenResourceURL(name);
		if(url != null) {
			return url;
		}
		url = parent.getResource(name);
		return url == null ? super.getResource(name) : url;
	}

	private URL getOverridenResourceURL(String name) {
		try {
			if(overridenResources.get(name) != null
			|| overridenClasses.get(classNameFromResource(name)) != null) {
				URL url = new URL("jar", "", -1, name, new ClassLoaderURLHandler(this));
				return url;
			}
		} catch (MalformedURLException e) {
		}
		return null;
	}

	public Enumeration<URL> findResources(String name) throws IOException {
		return parent.getResources(name);
	}

	public void addExclusion(String pkg) {
		exclusions.add(pkg + (pkg.endsWith(".") ? "" : "."));
	}

	public void addTransformExclusion(String pkg) {
		transformExclusions.add(pkg + (pkg.endsWith(".") ? "" : "."));
	}

	public void overrideClassSource(String name, String f) {
		overridenSource.put(className(name), f);
	}

	public void overrideResource(String name, byte[] data) {
		overridenResources.put(name, data);
	}

	public byte[] getResourceData(String path) {
		try {
			return Util.readStream(this.getResourceAsStream(path));
		} catch(IOException e) {
			return null;
		}
	}

	public Class<?> findClass(String name) throws ClassNotFoundException {
		assert name.contains(".");
		
		// Force wrap inject package
		if(name.startsWith("org.mcphackers.launchwrapper.inject.")) {
			return redefineClass(name);
		}

		for(String pkg : exclusions) {
			if(name.startsWith(pkg)) {
				return parent.loadClass(name);
			}
		}
		return redefineClass(name);
	}

	public void invokeMain(String launchTarget, String... args) {
		try {
			Class<?> mainClass = loadClass(launchTarget);
			mainClass.getDeclaredMethod("main", String[].class).invoke(null, (Object) args);
		} catch (InvocationTargetException e1) {
			if(e1.getCause() != null) {
				e1.getCause().printStackTrace();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void overrideClass(ClassNode node) {
		if(node == null)
			return;
		overridenClasses.put(className(node.name), node);
		classNodeCache.put(node.name, node);
	}

	// public void saveDebugDump(ClassNode node) {
	// 	if(debugOutput == null) {
	// 		return;
	// 	}
	// 	try {
	// 		File cls = new File(debugOutput, node.name + ".dump");
	// 		cls.getParentFile().mkdirs();
	// 		org.objectweb.asm.util.TraceClassVisitor trace = new org.objectweb.asm.util.TraceClassVisitor(new java.io.PrintWriter(new File(debugOutput, node.name + ".dump")));
	// 		node.accept(trace);
	// 	} catch (IOException e) {
	// 		e.printStackTrace();
	// 	}
	// }

	public void saveDebugClass(ClassNode node) {
		if(debugOutput == null) {
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
	 * @return parsed ClassNode
	 */
	public ClassNode getClass(String name) {
		assert !name.contains(".");
		ClassNode node = classNodeCache.get(name);
		if(node != null) {
			return node;
		}
		try {
			byte[] data = getTransformedClass(name);
			if(data == null) {
				return null;
			}
			ClassNode classNode = new ClassNode();
			ClassReader classReader = new ClassReader(data);
			classReader.accept(classNode, ClassReader.SKIP_FRAMES);
			classNodeCache.put(classNode.name, classNode);
			return classNode;
		} catch (IOException e) {
			return null;
		}
	}

	protected Class<?> redefineClass(String name) throws ClassNotFoundException {
		assert name.contains(".");
		String nodeName = classNodeName(name);
		if(tweaks != null) {
			boolean modified = false;
			for(LazyTweaker tweak : tweaks) {
				modified |= tweak.tweakClass(this, nodeName);
			}
			if(modified) {
				Launch.LOGGER.logDebug("Applying lazy tweak to %s", nodeName);
			}
		}
		ClassNode overridenNode = overridenClasses.get(name);
		if(overridenNode != null) {
			return redefineClass(overridenNode);
		}
		try {
			Class<?> loadedClass = findLoadedClass(getTransformedName(name));
			if(loadedClass != null) {
				Launch.LOGGER.logDebug("Attempted to reference unremapped class: %s", name);
				return loadedClass;
			}
			byte[] data = getTransformedClass(name);
			if(data == null) {
				throw new ClassNotFoundException(name);
			}
			return defineClass(getTransformedName(name), data);
		} catch (IOException e) {
			throw new ClassNotFoundException(name, e);
		}
	}

	private Class<?> redefineClass(ClassNode node) {
		if(node == null) {
			return null;
		}
		saveDebugClass(node);
		ClassWriter writer = new SafeClassWriter(this, COMPUTE_MAXS | COMPUTE_FRAMES);
		node.accept(writer);
		classNodeCache.remove(node.name);
		return defineClass(className(node.name), writer.toByteArray());
	}

	public String getTransformedName(String name) {
		if(bytecodeProvider != null) {
			return bytecodeProvider.getClassName(name);
		}
		return name;
	}
	
	public byte[] getTransformedClass(String name) throws IOException {
		for(String pkg : transformExclusions) {
			if(name.startsWith(pkg)) {
				return getClassBytes(name);
			}
		}
		if(bytecodeProvider != null) {
			return bytecodeProvider.getClassBytecode(name);
		}
		return getClassBytes(name);
	}

	public Class<?> getLoadedClass(String name) {
		return findLoadedClass(name);
	}

	public byte[] getClassBytes(String name) throws IOException {
		String className = classResourceName(name);
		InputStream is = this.getResourceAsStream(className);
		if(is == null) {
			is = parent.getResourceAsStream(className);
		}
		if(is == null) {
			return null;
		}
		return Util.readStream(is);
	}

	public void setLoaderTweakers(List<LazyTweaker> classLoaderTweaks) {
		tweaks = classLoaderTweaks;
	}

	private Class<?> defineClass(String name, byte[] classData) {
		int i = name.lastIndexOf('.');
		if (i != -1) {
			String pkgName = name.substring(0, i);
			if(getPackage(pkgName) == null) {
				definePackage(pkgName, null, null, null, null, null, null, null);
			}
		}
		return defineClass(name, classData, 0, classData.length, getProtectionDomain(name));
	}

	private ProtectionDomain getProtectionDomain(String name) {
		final URL resource = getResource(classResourceName(name));
		if(resource == null) {
			return null;
		}
		CodeSource codeSource;
		if(resource.getProtocol().equals("jar")) {
			String path = resource.getPath();
			if(path.startsWith("file:")) {
				path = path.substring("file:".length());
				int i = path.lastIndexOf('!');
				if(i != -1) {
					path = path.substring(0, i);
				}
			}
			if(overridenSource.get(name) != null) {
				path = overridenSource.get(name);
			}
			try {
				URL newResource = new URL("file", "", path);
				codeSource = new CodeSource(newResource, (Certificate[])null);
			} catch (MalformedURLException e) {
				codeSource = new CodeSource(resource, (Certificate[])null);
			}
		} else {
			codeSource = new CodeSource(resource, (Certificate[])null);
		}
		return new ProtectionDomain(codeSource, null);
	}

	private static ClassNode getSystemClass(Class<?> cls) {
		InputStream is = ClassLoader.getSystemResourceAsStream(classResourceName(cls.getName()));
		if(is == null)
			return null;
		try {
			ClassNode classNode = new ClassNode();
			ClassReader classReader = new ClassReader(is);
			classReader.accept(classNode, ClassReader.SKIP_FRAMES);
			return classNode;
		} catch (IOException e) {
			Util.closeSilently(is);
			return null;
		}
	}

	private static int getSupportedClassVersion() {
		ClassNode objectClass = getSystemClass(Object.class);
		return objectClass.version;
	}

	public static LaunchClassLoader instantiate() {
		if(INSTANCE != null) {
			throw new IllegalStateException("Can only have one instance of LaunchClassLoader!");
		}
		INSTANCE = new LaunchClassLoader(getSystemClassLoader());
		Thread.currentThread().setContextClassLoader(INSTANCE);
		return INSTANCE;
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
		if(resource.endsWith(".class")) {
			return resource.substring(0, resource.length() - 6);
		}
		return resource;
	}

}
