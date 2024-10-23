package org.mcphackers.launchwrapper.micromixin.util;

import java.io.Closeable;
import java.io.InputStream;
import java.util.Collection;

public interface ClasspathAdapter extends Closeable {

	InputStream getResource(String entry);

	Collection<String> getClasses();
}
