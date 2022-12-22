package org.mcphackers.launchwrapper.protocol;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.Hashtable;

import org.mcphackers.launchwrapper.util.Util;

public abstract class URLStreamHandlerProxy extends URLStreamHandler {
	protected URLStreamHandler parent;
	
	@Override
	protected final URLConnection openConnection(URL url, Proxy p) throws IOException {
		return openConnection(url);
	}
	
	protected URLConnection openConnection(URL url) throws IOException {
		if(parent == null) {
			return null;
		}
		return new URL(null, url.toString(), parent).openConnection();
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static void setURLStreamHandler(String protocol, URLStreamHandlerProxy handler) {
		handler.parent = getURLStreamHandler(protocol);
		try {
			Field handlersField = URL.class.getDeclaredField("handlers");
			Hashtable handlers = (Hashtable)Util.getStaticObjectUnsafe(handlersField);
			handlers.put(protocol, handler);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static URLStreamHandler getURLStreamHandler(String protocol) {
	    try {
	        URL url = new URL(protocol + ":");
	        Field handlerField = URL.class.getDeclaredField("handler");
	        return (URLStreamHandler)Util.getObjectUnsafe(url, handlerField);       
	    } catch (Exception e) {
	    	e.printStackTrace();
	        return null;
	    }
	}
}
