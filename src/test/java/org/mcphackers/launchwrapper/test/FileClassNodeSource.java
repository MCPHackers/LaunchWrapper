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
import org.mcphackers.rdi.util.NodeHelper;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

public class FileClassNodeSource implements ClassNodeSource, Closeable {
    private final ZipFile source;
	private Map<String, ClassNode> classNodeCache = new HashMap<String, ClassNode>();
	private File debugOutput;

    public FileClassNodeSource(File jarFile) throws ZipException, IOException {
        source = new ZipFile(jarFile);
    }

	public void setDebugOutput(File directory) {
		debugOutput = directory;
	}

	private void saveDebugClass(ClassNode node) {
		if(debugOutput == null) {
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
		if(entry == null) {
			return null;
		}
		InputStream is = source.getInputStream(entry);
		if(is != null) {
			return is;
		}
		return null;
	}

    public ClassNode getClass(String name) {
		ClassNode node = classNodeCache.get(classNodeName(name));
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

	public FieldNode getField(String owner, String name, String desc) {
		return NodeHelper.getField(getClass(owner), name, desc);
	}

	public MethodNode getMethod(String owner, String name, String desc) {
		return NodeHelper.getMethod(getClass(owner), name, desc);
	}

    public void overrideClass(ClassNode node) {
		saveDebugClass(node);
    }

    public void close() throws IOException {
        source.close();
    }
    
}