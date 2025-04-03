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

import org.mcphackers.launchwrapper.Launch;
import org.mcphackers.launchwrapper.util.UnsafeUtils;

public abstract class URLStreamHandlerProxy extends URLStreamHandler {
	private static final Map<String, URLStreamHandler> DEFAULT_HANDLERS = new HashMap<String, URLStreamHandler>();
	private static final Hashtable<String, URLStreamHandler> HANDLERS = getHandlers();

	protected URLStreamHandler parent;

	@Override
	protected final URLConnection openConnection(URL url, Proxy p) throws IOException {
		return openConnection(url);
	}

	@Override
	protected URLConnection openConnection(URL url) throws IOException {
		return openDirectConnection(url);
	}

	public static URLConnection openDirectConnection(URL url) throws IOException {
		URLConnection connection = new URL(null, url.toExternalForm(), getDefaultURLStreamHandler(url.getProtocol())).openConnection();
		connection.addRequestProperty("User-Agent", "LaunchWrapper/" + Launch.VERSION);
		return connection;
	}

	public static void setURLStreamHandler(String protocol, URLStreamHandlerProxy handler) {
		handler.parent = getURLStreamHandler(protocol);
		HANDLERS.put(protocol, handler);
	}

	private static URLStreamHandler getDefaultURLStreamHandler(String protocol) {
		URLStreamHandler handler = DEFAULT_HANDLERS.get(protocol);
		if (handler != null) {
			return handler;
		}
		try {
			URL url = new URL(protocol + ":");
			Field handlerField = URL.class.getDeclaredField("handler");
			handler = (URLStreamHandler)UnsafeUtils.getObject(url, handlerField);
			DEFAULT_HANDLERS.put(protocol, handler);
			return handler;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public static URLStreamHandler getURLStreamHandler(String protocol) {
		URLStreamHandler handler = DEFAULT_HANDLERS.get(protocol);
		if (handler == null) {
			return getDefaultURLStreamHandler(protocol);
		}
		return HANDLERS.get(protocol);
	}

	@SuppressWarnings("unchecked")
	private static Hashtable<String, URLStreamHandler> getHandlers() {
		try {
			Field handlersField = URL.class.getDeclaredField("handlers");
			return (Hashtable<String, URLStreamHandler>)UnsafeUtils.getStaticObject(handlersField);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
}
