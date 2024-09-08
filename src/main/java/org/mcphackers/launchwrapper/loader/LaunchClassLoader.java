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
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.mcphackers.launchwrapper.tweak.ClassLoaderTweak;
import org.mcphackers.launchwrapper.util.ClassNodeSource;
import org.mcphackers.launchwrapper.util.ResourceSource;
import org.mcphackers.launchwrapper.util.Util;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.util.TraceClassVisitor;

// URLClassLoader is required to support ModLoader loading mods from mod folder
public class LaunchClassLoader extends URLClassLoader implements ClassNodeSource, ResourceSource {

	public static final int CLASS_VERSION = getSupportedClassVersion();
	private static LaunchClassLoader INSTANCE;

	private ClassLoader parent;
	private ClassLoaderTweak tweak;
	private Set<String> exceptions = new HashSet<String>();
	private Set<String> ignoreExceptions = new HashSet<String>();
	/** Keys should contain dots */
	Map<String, ClassNode> overridenClasses = new HashMap<String, ClassNode>();
	Map<String, byte[]> overridenResources = new HashMap<String, byte[]>();
	Map<String, String> overridenSource = new HashMap<String, String>();
	/** Keys should contain slashes */
	private Map<String, ClassNode> classNodeCache = new HashMap<String, ClassNode>();
	private File debugOutput;

	public LaunchClassLoader(ClassLoader parent) {
		super(new URL[0], null);
		this.parent = parent;
	}

	public void setClassPath(URL[] urls) {
		for(URL url : urls) {
			addURL(url);
		}
	}

	public void setDebugOutput(File directory) {
		debugOutput = directory;
	}

    public void addURL(URL url) {
    	super.addURL(url);
    }

	public URL findResource(String name) {
		URL url = getOverridenResourceURL(name);
		if(url != null) {
			return url;
		}
		url = super.findResource(name);
		return url == null ? parent.getResource(name) : url;
	}

	public URL getResource(String name) {
		URL url = getOverridenResourceURL(name);
		if(url != null) {
			return url;
		}
		url = super.getResource(name);
		return url == null ? parent.getResource(name) : url;
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

	public Class<?> findClass(String name) throws ClassNotFoundException {
		assert name.contains(".");
		if(name.startsWith("java.")) {
			return parent.loadClass(name);
		}
		Class<?> cls = null;

		if(overridenSource.get(name) != null) {
			return transformedClass(name);
		}
		outer:
		for(String pkg : exceptions) {
			for(String pkg2 : ignoreExceptions) {
				if(name.startsWith(pkg2)) {
					continue outer;
				}
			}
			if(name.startsWith(pkg)) {
				cls = parent.loadClass(name);
				if(cls != null) {
					return cls;
				}
			}
		}
		cls = transformedClass(name);
		if(cls != null) {
			return cls;
		}
		throw new ClassNotFoundException(name);
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

	public void addException(String pkg) {
		exceptions.add(pkg + ".");
	}

	public void removeException(String pkg) {
		ignoreExceptions.add(pkg + ".");
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
				codeSource = new CodeSource(newResource, new Certificate[0]);
			} catch (MalformedURLException e) {
				codeSource = new CodeSource(resource, new Certificate[0]);
			}
		} else {
			codeSource = new CodeSource(resource, new Certificate[0]);
		}
		return new ProtectionDomain(codeSource, null);
	}

	public void overrideClass(ClassNode node) {
		if(node == null)
			return;
		saveDebugClass(node);
		overridenClasses.put(className(node.name), node);
		classNodeCache.put(node.name, node);
	}

	public void saveDebugClass(ClassNode node) {
		if(debugOutput == null) {
			return;
		}
		try {
			File cls = new File(debugOutput, node.name + ".class");
			cls.getParentFile().mkdirs();
			TraceClassVisitor trace = new TraceClassVisitor(new java.io.PrintWriter(new File(debugOutput, node.name + ".dump")));
			node.accept(trace);
			ClassWriter writer = new SafeClassWriter(this, COMPUTE_MAXS | COMPUTE_FRAMES);
			node.accept(writer);
			byte[] classData = writer.toByteArray();
			FileOutputStream fos = new FileOutputStream(cls);
			fos.write(classData);
			fos.close();
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
		if(name.startsWith("java/")) {
			return null;
		}
		ClassNode node = classNodeCache.get(name);
		if(node != null) {
			return node;
		}
		try {
			InputStream is = getClassAsStream(name);
			if(is == null) {
				return null;
			}
			ClassNode classNode = new ClassNode();
			ClassReader classReader = new ClassReader(is);
			classReader.accept(classNode, 0);
			classNodeCache.put(classNode.name, classNode);
			return classNode;
		} catch (IOException e) {
			return null;
		}
	}

	protected Class<?> redefineClass(String name) throws ClassNotFoundException {
		assert name.contains(".");
		String nodeName = classNodeName(name);
		if(tweak != null) {
			if(tweak.tweakClass(this, nodeName)) {
				ClassNode tweakedNode = getClass(nodeName);
				saveDebugClass(tweakedNode);
				return redefineClass(tweakedNode);
			}
		}
		try {
			InputStream is = getClassAsStream(name);
			if(is == null) {
				throw new ClassNotFoundException(name);
			}
			byte[] classData = Util.readStream(is);
			return defineClass(name, classData);
		} catch (IOException e) {
			throw new ClassNotFoundException(name, e);
		}
	}

	private Class<?> redefineClass(ClassNode node) {
		if(node == null) {
			return null;
		}
		ClassWriter writer = new SafeClassWriter(this, COMPUTE_MAXS | COMPUTE_FRAMES);
		node.accept(writer);
		byte[] classData = writer.toByteArray();
		return defineClass(className(node.name), classData);
	}

	private static String className(String nameWithSlashes) {
		return nameWithSlashes.replace('/', '.');
	}

	private static String classNodeName(String nameWithDots) {
		return nameWithDots.replace('.', '/');
	}

	private static String classResourceName(String name) {
		return classNodeName(name) + ".class";
	}

	static String classNameFromResource(String resource) {
		if(resource.endsWith(".class")) {
			return resource.substring(0, resource.length() - 6);
		}
		return resource;
	}

	private InputStream getClassAsStream(String name) {
		String className = classResourceName(name);
		InputStream is = super.getResourceAsStream(className);
		if(is != null) {
			return is;
		}
		return parent.getResourceAsStream(className);
	}

	private Class<?> transformedClass(String name) throws ClassNotFoundException {
		assert name.contains(".");
		ClassNode transformed = overridenClasses.get(name);
		if(transformed != null) {
			if(tweak != null) {
				if(tweak.tweakClass(this, classNodeName(name))) {
					transformed = overridenClasses.get(name);
					saveDebugClass(transformed);
				}
			}
			return redefineClass(transformed);
		}
		return redefineClass(name);
	}

	public void setLoaderTweak(ClassLoaderTweak classLoaderTweak) {
		tweak = classLoaderTweak;
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

	private static ClassNode getSystemClass(Class<?> cls) {
		InputStream is = ClassLoader.getSystemResourceAsStream(classResourceName(cls.getName()));
		if(is == null)
			return null;
		try {
			ClassNode classNode = new ClassNode();
			ClassReader classReader = new ClassReader(is);
			classReader.accept(classNode, 0);
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
		return INSTANCE = new LaunchClassLoader(getSystemClassLoader());
	}

}
