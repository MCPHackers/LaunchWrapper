package org.mcphackers.launchwrapper.applet;
import java.awt.Dimension;
import java.awt.Panel;
import java.net.URL;
import java.util.Locale;

public class Applet extends Panel {
	private AppletStub stub;

	public final void setStub(AppletStub stub) {
		this.stub = stub;
	}

	public boolean isActive() {
		if (stub != null) {
			return stub.isActive();
		} else {
			return false;
		}
	}

	public URL getDocumentBase() {
		return stub.getDocumentBase();
	}

	public URL getCodeBase() {
		return stub.getCodeBase();
	}

	public String getParameter(String name) {
		return stub.getParameter(name);
	}

	@SuppressWarnings("deprecation")
	@Override
	public void resize(int width, int height) {
		Dimension d = size();

		if ((d.width != width) || (d.height != height)) {
			super.resize(width, height);
		}
	}

	@SuppressWarnings("deprecation")
	@Override
	public void resize(Dimension d) {
		resize(d.width, d.height);
	}

	@Override
	public boolean isValidateRoot() {
		return true;
	}

	@Override
	public Locale getLocale() {
		Locale locale = super.getLocale();

		if (locale == null) {
			return Locale.getDefault();
		}

		return locale;
	}

	public void init() {
	}

	public void start() {
	}

	public void stop() {
	}

	public void destroy() {
	}
}
