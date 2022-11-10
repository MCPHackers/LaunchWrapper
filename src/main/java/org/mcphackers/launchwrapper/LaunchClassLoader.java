package org.mcphackers.launchwrapper;

import static org.objectweb.asm.ClassWriter.*;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

public class LaunchClassLoader extends ClassLoader {
	
	private ClassLoader parent;
	private Map<String, Class<?>> classes = new HashMap<String, Class<?>>();
	
	public LaunchClassLoader(ClassLoader parent) {
		super(null);
		this.parent = parent;
	}
	
	public URL getResource(String name) {
		return parent.getResource(name);
	}
	
	@Override
	public Class<?> findClass(String name) throws ClassNotFoundException {
		Class<?> cls = findClassOrLoad(name);
		if(cls == null) {
            throw new ClassNotFoundException(name);
		}
        return cls;
	}
	
	protected Class<?> redefineClass(String name) {
    	InputStream is = parent.getResourceAsStream(name.replace('.', '/') + ".class");
    	if(is == null) return null;
    	byte[] classData;
		try {
			classData = Util.readStream(is);
		} catch (IOException e) {
	        Util.closeSilently(is);
			return null;
		}
        Util.closeSilently(is);
		Class<?> definedClass = defineClass(name, classData, 0, classData.length);
		classes.put(name, definedClass);
		return definedClass;
	}
	
	public Class<?> redefineClass(ClassNode node) {
		Class<?> definedClass = defineClass(node);
		classes.put(className(node.name), definedClass);
		return definedClass;
	}
	
	private Class<?> defineClass(ClassNode node) throws ClassFormatError {
		if(node == null) {
			return null;
		}
		ClassWriter writer = new ClassWriter(COMPUTE_MAXS | COMPUTE_FRAMES);
		node.accept(writer);
		byte[] classData = writer.toByteArray();
		return defineClass(className(node.name), classData, 0, classData.length);
	}
	
	private static String className(String nameWithSlashes) {
		return nameWithSlashes.replace('/', '.');
	}

	private Class<?> findClassOrLoad(String name) {
		Class<?> cls = classes.get(name);
		if(cls != null) {
			return cls;
		}
		cls = redefineClass(name);
        return cls;
	}

	public static LaunchClassLoader instantiate() {
		LaunchClassLoader loader = new LaunchClassLoader(getSystemClassLoader());
		Thread.currentThread().setContextClassLoader(loader);
		return loader;
	}

}
