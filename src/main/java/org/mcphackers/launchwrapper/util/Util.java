package org.mcphackers.launchwrapper.util;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.mcphackers.launchwrapper.Launch;

public final class Util {

	private Util() {
	}

	public static void closeSilently(Closeable closeable) {
		if (closeable != null) {
			try {
				closeable.close();
			} catch (IOException ignored) {
			}
		}
	}

	public static List<File> getSource(ClassLoader loader, String resPath) {
		List<File> sources = new ArrayList<File>();
		try {
			Enumeration<URL> enumeration = loader.getResources(resPath);
			while (enumeration.hasMoreElements()) {
				URL resource = enumeration.nextElement();
				if (resource.getProtocol().equals("jar")) {
					String path = resource.getPath();
					if (path.startsWith("file:")) {
						path = path.substring("file:".length());
						int i = path.lastIndexOf('!');
						if (i != -1) {
							path = path.substring(0, i);
						}
					}
					sources.add(new File(path));
				} else if (resource.getPath().endsWith(resPath)) {
					sources.add(new File(resource.getPath().substring(0, resource.getPath().length() - resPath.length() - 1)));
				}
			}
		} catch (IOException e) {
		}
		return sources;
	}

	public static byte[] getResource(String path) {
		try {
			return Util.readStream(Launch.class.getResourceAsStream(path));
		} catch (IOException e) {
			return null;
		}
	}

	public static byte[] readStream(InputStream stream) throws IOException {
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();

		int nRead;
		byte[] data = new byte[16384];

		while ((nRead = stream.read(data, 0, data.length)) != -1) {
			buffer.write(data, 0, nRead);
		}
		stream.close();
		return buffer.toByteArray();
	}

	public static void copyStream(InputStream stream1, OutputStream stream2) throws IOException {
		int nRead;
		byte[] data = new byte[16384];

		while ((nRead = stream1.read(data, 0, data.length)) != -1) {
			stream2.write(data, 0, nRead);
		}
		stream1.close();
		stream2.close();
	}

	public static String getSHA256(InputStream is) throws IOException {
		MessageDigest md;
		try {
			md = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			throw new IOException(e.getMessage());
		}
		BufferedInputStream bs = new BufferedInputStream(is);
		byte[] buffer = new byte[1024];
		int bytesRead;

		while ((bytesRead = bs.read(buffer, 0, buffer.length)) != -1) {
			md.update(buffer, 0, bytesRead);
		}
		byte[] digest = md.digest();

		StringBuilder sb = new StringBuilder();
		for (byte bite : digest) {
			sb.append(Integer.toString((bite & 255) + 256, 16).substring(1).toLowerCase());
		}
		bs.close();
		return sb.toString();
	}

	public static String getSHA1(InputStream is) throws IOException {
		MessageDigest md;
		try {
			md = MessageDigest.getInstance("SHA-1");
		} catch (NoSuchAlgorithmException e) {
			throw new IOException(e.getMessage());
		}
		BufferedInputStream bs = new BufferedInputStream(is);
		byte[] buffer = new byte[1024];
		int bytesRead;

		while ((bytesRead = bs.read(buffer, 0, buffer.length)) != -1) {
			md.update(buffer, 0, bytesRead);
		}
		byte[] digest = md.digest();

		StringBuilder sb = new StringBuilder();
		for (byte bite : digest) {
			sb.append(Integer.toString((bite & 255) + 256, 16).substring(1).toLowerCase());
		}
		bs.close();
		return sb.toString();
	}

	public static Map<String, String> queryMap(URL url) throws UnsupportedEncodingException {
		Map<String, String> queryMap = new HashMap<String, String>();
		String query = url.getQuery();
		if (query != null) {
			String[] pairs = query.split("&");
			for (String pair : pairs) {
				final int idx = pair.indexOf("=");
				final String key = idx > 0 ? URLDecoder.decode(pair.substring(0, idx), "UTF-8") : pair;
				final String value = idx > 0 && pair.length() > idx + 1 ? URLDecoder.decode(pair.substring(idx + 1), "UTF-8") : null;
				queryMap.put(key, value);
			}
		}
		return queryMap;
	}
}
