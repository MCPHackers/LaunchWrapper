package org.mcphackers.launchwrapper.loader;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

public class SafeClassWriter extends ClassWriter {
	private final ClassLoader classLoader;

	public SafeClassWriter(final int flags) {
		this(null, flags);
	}

	public SafeClassWriter(ClassLoader parent, int flags) {
		this(parent, null, flags);
	}

	public SafeClassWriter(ClassLoader parent, ClassReader classReader, int flags) {
		super(classReader, flags);
		classLoader = parent == null ? ClassLoader.getSystemClassLoader() : parent;
	}

	protected ClassLoader getClassLoader() {
		return classLoader;
	}
}
