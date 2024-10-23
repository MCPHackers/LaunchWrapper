package org.mcphackers.launchwrapper.micromixin.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

public class DirectoryClasspath implements ClasspathAdapter {
	Path dir;

	public DirectoryClasspath(Path dir) {
		this.dir = dir;
	}

	@Override
	public void close() throws IOException {
	}

	@Override
	public InputStream getResource(String entry) {
		Path f = dir.resolve(entry);
		if (!Files.isRegularFile(f)) {
			return null;
		}
		try {
			return Files.newInputStream(f);
		} catch (IOException e) {
			return null;
		}
	}

	@Override
	public Collection<String> getClasses() {
		try {
			return Files.walk(dir)
				.filter(p -> p.toString().endsWith(".class"))
				.map(p -> p.relativize(dir).toString())
				.map(s -> s.substring(0, s.length() - 6))
				.collect(Collectors.toList());
		} catch (IOException e) {
			e.printStackTrace();
			return Collections.emptyList();
		}
	}
}
