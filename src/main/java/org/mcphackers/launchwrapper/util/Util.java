package org.mcphackers.launchwrapper.util;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

import sun.misc.Unsafe;

@SuppressWarnings("restriction")
public final class Util {
	
	private static final Unsafe theUnsafe = getUnsafe();
	
	private static Unsafe getUnsafe() {
		try {
			final Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
	        unsafeField.setAccessible(true);
	        final Unsafe unsafe = (Unsafe) unsafeField.get(null);
			return unsafe;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public static void setStaticBooleanUnsafe(final Field field, boolean value) {
        final Object staticFieldBase = theUnsafe.staticFieldBase(field);
        final long staticFieldOffset = theUnsafe.staticFieldOffset(field);
        theUnsafe.putBoolean(staticFieldBase, staticFieldOffset, value);
	}

	public static void setStaticObjectUnsafe(final Field field, Object value) {
        final Object staticFieldBase = theUnsafe.staticFieldBase(field);
        final long staticFieldOffset = theUnsafe.staticFieldOffset(field);
        theUnsafe.putObject(staticFieldBase, staticFieldOffset, value);
	}

	public static Object getStaticObjectUnsafe(final Field field) {
        final Object staticFieldBase = theUnsafe.staticFieldBase(field);
        final long staticFieldOffset = theUnsafe.staticFieldOffset(field);
        return theUnsafe.getObject(staticFieldBase, staticFieldOffset);
	}

	public static Object getObjectUnsafe(final Object base, final Field field) {
        final long fieldOffset = theUnsafe.objectFieldOffset(field);
        return theUnsafe.getObject(base, fieldOffset);
	}

    public static void closeSilently(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException ignored) {
            }
        }
    }

    public static byte[] readStream(InputStream stream) throws IOException {
    	ByteArrayOutputStream buffer = new ByteArrayOutputStream();

    	int nRead;
    	byte[] data = new byte[16384];

    	while ((nRead = stream.read(data, 0, data.length)) != -1) {
    		buffer.write(data, 0, nRead);
    	}

    	return buffer.toByteArray();
    }

	public static URL replaceHost(URL url, String hostName, int port) {
		try {
			return new URL(url.getProtocol(), hostName, port, url.getFile());
		} catch (MalformedURLException e) {
			e.printStackTrace();
			return null;
		}
	}

	public static Map<String, String> queryMap(URL url) throws UnsupportedEncodingException {
		Map<String, String> queryMap = new HashMap<String, String>();
		String query = url.getQuery();
		if(query != null) {
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
