package org.mcphackers.launchwrapper.protocol;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.mcphackers.launchwrapper.util.UnsafeUtils;

public abstract class URLStreamHandlerProxy extends URLStreamHandler {
	private static final Map<String, URLStreamHandler> DEFAULT_HANDLERS = new HashMap<String, URLStreamHandler>();

	protected URLStreamHandler parent;

	@Override
	protected final URLConnection openConnection(URL url, Proxy p) throws IOException {
		return openConnection(url);
	}

	protected URLConnection openConnection(URL url) throws IOException {
		if(parent == null) {
			return null;
		}
		return new URL(null, url.toExternalForm(), parent).openConnection();
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static void setURLStreamHandler(String protocol, URLStreamHandlerProxy handler) {
		handler.parent = getURLStreamHandler(protocol);
		try {
			Field handlersField = URL.class.getDeclaredField("handlers");
			Hashtable handlers = (Hashtable) UnsafeUtils.getStaticObject(handlersField);
			handlers.put(protocol, handler);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static URLStreamHandler getURLStreamHandler(String protocol) {
		URLStreamHandler handler = DEFAULT_HANDLERS.get(protocol);
		if(handler != null) {
			return handler;
		}
		try {
			URL url = new URL(protocol + ":");
			Field handlerField = URL.class.getDeclaredField("handler");
			handler = (URLStreamHandler) UnsafeUtils.getObject(url, handlerField);
			DEFAULT_HANDLERS.put(protocol, handler);
			return handler;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
}
