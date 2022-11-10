package org.mcphackers.launchwrapper;

import java.applet.Applet;
import java.applet.AppletStub;
import java.net.MalformedURLException;
import java.net.URL;

public class AppletWrapper extends Applet implements AppletStub {
    private static final long serialVersionUID = 1L;
    
    private Object instance;
    private LaunchTarget launchTarget;
    
    public AppletWrapper(LaunchTarget launchTarget) {
    	this.launchTarget = launchTarget;
	}
    
    public void setInstance(Object instance) {
    	this.instance = instance;
    }

    public void appletResize(int width, int height) {
    	if(instance != null) {
	    	launchTarget.setWidth(instance, width);
	    	launchTarget.setHeight(instance, height);
    	}
    }

    @Override
    public boolean isActive() {
        return true;
    }

    @Override
    public URL getDocumentBase() {
        try {
            return new URL("http://www.minecraft.net/game/");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public URL getCodeBase() {
        try {
            return new URL("http://www.minecraft.net/game/");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public String getParameter(String paramName) {
    	if(paramName.equals("stand-alone")) {
    		return "true";
    	}
        System.err.println("Client asked for parameter: " + paramName);
        return null;
    }
}
