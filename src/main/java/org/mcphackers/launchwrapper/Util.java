package org.mcphackers.launchwrapper;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;

public final class Util {
	
	public static void setField(Object instance, Field field, Object value) {
		if(field != null) {
			try {
//				if(!field.isAccessible()) {
//					field.setAccessible(true);
//				}
				field.set(instance, value);
			}
			catch (Exception e) { }
		}
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

}
