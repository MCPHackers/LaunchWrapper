package org.mcphackers.launchwrapper.test;

import static org.objectweb.asm.ClassWriter.COMPUTE_MAXS;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import org.mcphackers.launchwrapper.util.ClassNodeSource;
import org.mcphackers.launchwrapper.util.ResourceSource;
import org.mcphackers.launchwrapper.util.Util;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

public class FileClassNodeSource implements ClassNodeSource, ResourceSource, Closeable {
	private final ZipFile source;
	private final Map<String, ClassNode> classNodeCache = new HashMap<String, ClassNode>();
	private File debugOutput;

	public FileClassNodeSource(File jarFile) throws ZipException, IOException {
		source = new ZipFile(jarFile);
	}

	public void setDebugOutput(File directory) {
		debugOutput = directory;
	}

	private void saveDebugClass(ClassNode node) {
		if (debugOutput == null) {
			return;
		}
		ClassWriter writer = new ClassWriter(COMPUTE_MAXS);
		node.accept(writer);
		byte[] classData = writer.toByteArray();
		File cls = new File(debugOutput, node.name + ".class");
		cls.getParentFile().mkdirs();
		try {
			FileOutputStream fos = new FileOutputStream(cls);
			fos.write(classData);
			fos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static String classNodeName(String nameWithDots) {
		return nameWithDots.replace('.', '/');
	}

	private static String classResourceName(String name) {
		return name.replace('.', '/') + ".class";
	}

	private InputStream getClassAsStream(String name) throws IOException {
		String className = classResourceName(name);
		ZipEntry entry = source.getEntry(className);
		if (entry == null) {
			return null;
		}
		InputStream is = source.getInputStream(entry);
		if (is != null) {
			return is;
		}
		return null;
	}

	@Override
	public byte[] getResourceData(String path) {
		ZipEntry entry = source.getEntry(path);
		if (entry == null) {
			return null;
		}
		try {
			InputStream is = source.getInputStream(entry);
			if (is != null) {
				return Util.readStream(is);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public ClassNode getClass(String name) {
		ClassNode node = classNodeCache.get(classNodeName(name));
		if (node != null) {
			return node;
		}
		try {
			InputStream is = getClassAsStream(name);
			if (is == null) {
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

	@Override
	public void overrideClass(ClassNode node) {
		saveDebugClass(node);
	}

	@Override
	public void overrideResource(String path, byte[] data) {
	}

	@Override
	public void close() throws IOException {
		source.close();
	}
}
