package org.mcphackers.launchwrapper.inject;

import java.applet.Applet;
import java.applet.AppletStub;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Image;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
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
    
    public static void startApplet(Class<? extends Applet> appletClass, int width, int height, String title) {
    	startApplet(appletClass, width, height, title, null);
    }
    
    public static void startApplet(Class<? extends Applet> appletClass, int width, int height, String title, Image icon) {
    	try {
			final Applet applet = appletClass.newInstance();
			if(applet != null) {
				applet.setStub(new AppletWrapper());
			}
			final Frame frame = new Frame(title);
			frame.setBackground(Color.BLACK);
			frame.setIconImage(icon);
			frame.setLayout(new BorderLayout());
			frame.add(applet, "Center");
			frame.setPreferredSize(new Dimension(width, height));
			frame.pack();
			frame.setLocationRelativeTo(null);
			frame.setVisible(true);
			frame.addWindowListener(new WindowAdapter() {
				public void windowClosing(WindowEvent event) {
					applet.stop();
					frame.dispose();
				}
			});
			applet.start();
    	} catch (Exception e) {
    		e.printStackTrace();
    	};
    }
}
