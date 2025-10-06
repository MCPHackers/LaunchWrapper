package org.mcphackers.launchwrapper.applet;

import java.net.URL;

public interface AppletStub {
	boolean isActive();

	URL getDocumentBase();

	URL getCodeBase();

	String getParameter(String name);

	// void appletResize(int width, int height);
}
