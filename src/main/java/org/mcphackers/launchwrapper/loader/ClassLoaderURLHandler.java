package org.mcphackers.launchwrapper.loader;

import static org.objectweb.asm.ClassWriter.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.jar.JarFile;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

public class ClassLoaderURLHandler extends URLStreamHandler {

	private final LaunchClassLoader classLoader;
	private final String resourcePath;

	public ClassLoaderURLHandler(LaunchClassLoader classLoader, String resourcePath) {
		this.classLoader = classLoader;
		this.resourcePath = resourcePath;
	}

	class ClassLoaderURLConnection extends JarURLConnection {
		protected ClassLoaderURLConnection(URL url) throws MalformedURLException {
			super(url);
		}

		@Override
		public void connect() throws IOException {
		}

		@Override
		public InputStream getInputStream() throws IOException {
			byte[] data = classLoader.overridenResources.get(resourcePath);
			if (data != null) {
				return new ByteArrayInputStream(data);
			}
			ClassNode node = classLoader.overridenClasses.get(LaunchClassLoader.classNameFromResource(resourcePath));
			if (node == null) {
				InputStream is = classLoader.getOriginalURL(resourcePath).openStream();
				return is;
			}
			ClassWriter writer = new SafeClassWriter(classLoader, COMPUTE_MAXS);
			node.accept(writer);
			data = writer.toByteArray();
			return new ByteArrayInputStream(data);
		}

		@Override
		public JarFile getJarFile() throws IOException {
			return null;
		}
	}

	@Override
	protected URLConnection openConnection(URL url) throws IOException {
		return new ClassLoaderURLConnection(url);
	}
}
