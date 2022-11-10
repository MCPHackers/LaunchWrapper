package org.mcphackers.launchwrapper;

import java.applet.Applet;
import java.applet.AppletStub;
import java.net.MalformedURLException;
import java.net.URL;

public class AppletWrapper extends Applet implements AppletStub {
    private static final long serialVersionUID = 1L;

    public void appletResize(int width, int height) {
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
