package org.mcphackers.launchwrapper.loader;

import static org.objectweb.asm.ClassWriter.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import org.mcphackers.launchwrapper.Util;
import org.mcphackers.launchwrapper.inject.ClassNodeSource;
import org.mcphackers.launchwrapper.inject.InjectUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

public class LaunchClassLoader extends ClassLoader implements ClassNodeSource {
	
	private static LaunchClassLoader INSTANCE;

	private ClassLoader parent;
	private Map<String, Class<?>> exceptions = new HashMap<String, Class<?>>();
	/** Keys should contain dots */
	private Map<String, Class<?>> classes = new HashMap<String, Class<?>>();
	/** Keys should contain dots */
	private Map<String, ClassNode> overridenClasses = new HashMap<String, ClassNode>();
	/** Keys should contain slashes */
	private Map<String, ClassNode> classNodeCache = new HashMap<String, ClassNode>();

	public LaunchClassLoader(ClassLoader parent) {
		super(null);
		this.parent = parent;
	}

	public URL getResource(String name) {
		return parent.getResource(name);
	}

	public Enumeration<URL> getResources(String name) throws IOException {
		return parent.getResources(name);
	}

	public Class<?> findClass(String name) throws ClassNotFoundException {
		name = className(name);
		Class<?> cls = exceptions.get(name);
		if(cls != null) {
            return cls;
		}
		cls = transformedClass(name);
		if(cls != null) {
	        return cls;
		}
        throw new ClassNotFoundException(name);
	}

	public void invokeMain(String launchTarget, String... args) {
		try {
			Class<?> mainClass = findClass(launchTarget);
			mainClass.getDeclaredMethod("main", String[].class).invoke(null, (Object) args);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	protected Class<?> redefineClass(String name) {
		byte[] classData = getClassAsBytes(name);
		if(classData == null) return null;
		Class<?> definedClass = defineClass(name, classData, 0, classData.length);
		classes.put(name, definedClass);
		return definedClass;
	}

	public void overrideClass(ClassNode node) {
		if(true) {
			File saveFolder = new File("C:/Users/User/Desktop/debug");
			ClassWriter writer = new ClassWriter(COMPUTE_MAXS | COMPUTE_FRAMES);
			node.accept(writer);
			byte[] classData = writer.toByteArray();
			File cls = new File(saveFolder, node.name + ".class");
			cls.getParentFile().mkdirs();
			try {
				FileOutputStream fos = new FileOutputStream(cls);
				fos.write(classData);
				fos.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		overridenClasses.put(className(node.name), node);
		classNodeCache.put(node.name, node);
	}

	/**
	 * @param name can be specified either as net.minecraft.client.Minecraft or as net/minecraft/client/Minecraft
	 * @return parsed ClassNode
	 */
	public ClassNode getClass(String name) {
		ClassNode node = classNodeCache.get(classNodeName(name));
		if(node != null) {
			return node;
		}
		byte[] classData = getClassAsBytes(name);
		if(classData == null) return null;
		ClassNode classNode = new ClassNode();
	    ClassReader classReader = new ClassReader(classData);
	    classReader.accept(classNode, ClassReader.EXPAND_FRAMES);
	    classNodeCache.put(classNode.name, classNode);
	    return classNode;
	}

	public FieldNode getField(String owner, String name, String desc) {
	    return InjectUtils.getField(getClass(owner), name, desc);
	}

	public MethodNode getMethod(String owner, String name, String desc) {
	    return InjectUtils.getMethod(getClass(owner), name, desc);
	}

	private Class<?> redefineClass(ClassNode node) {
		if(node == null) {
			return null;
		}
		ClassWriter writer = new ClassWriter(COMPUTE_MAXS | COMPUTE_FRAMES);
		node.accept(writer);
		byte[] classData = writer.toByteArray();
		Class<?> definedClass = defineClass(className(node.name), classData, 0, classData.length);
		classes.put(className(node.name), definedClass);
		return definedClass;
	}

	private static String className(String nameWithSlashes) {
		return nameWithSlashes.replace('/', '.');
	}

	private static String classNodeName(String nameWithDots) {
		return nameWithDots.replace('.', '/');
	}

	private byte[] getClassAsBytes(String name) {
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
        return classData;
	}

	private Class<?> transformedClass(String name) {
		Class<?> cls = classes.get(name);
		if(cls != null) {
			return cls;
		}
		ClassNode transformed = overridenClasses.get(name);
		if(transformed != null) {
	        return redefineClass(transformed);
		}
        return redefineClass(name);
	}
	
	public void addException(Class<?> cls) {
		if(cls == null) return;
		exceptions.put(cls.getName(), cls);
	}

	public static LaunchClassLoader instantiate() {
		if(INSTANCE != null) {
			throw new IllegalStateException("Can only have one instance of LaunchClassLoader!");
		}
		INSTANCE = new LaunchClassLoader(getSystemClassLoader());
		Thread.currentThread().setContextClassLoader(INSTANCE);
		return INSTANCE;
	}

}
