package org.mcphackers.launchwrapper.protocol;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import org.mcphackers.launchwrapper.Launch;

public class ResourceIndexURLConnection extends URLConnection {
	private static final String[] RESOURCES = new String[] {"/resources/", "/MinecraftResources/"};
	private boolean xmlMode;

	public ResourceIndexURLConnection(URL u, boolean xmlMode) {
		super(u);
		this.xmlMode = xmlMode;
	}

	@Override
	public void connect() throws IOException {}

	@Override
	public InputStream getInputStream() throws IOException {
		File assetsDir = new File(Launch.INSTANCE.config.gameDir.get(), "resources");
		String path = url.getPath();
		for(String template : RESOURCES) {
			if(path.startsWith(template)) {
				String assetName = path.substring(template.length());
				if(assetName.isEmpty()) {
					File indexFile;
					if (xmlMode) {
						indexFile = new File(assetsDir, "index.xml");
					} else {
						indexFile = new File(assetsDir, "index.txt");
					}
					if(indexFile.exists()) {
						return new FileInputStream(indexFile);
					} else {
						//TODO download from another host if missing locally
						throw new FileNotFoundException("Missing resource index");
					}
				}
				//TODO download from another host at all times
				// it only requests it if (file.length() != resourceIndexFileLength || !file.exists())
				File asset = new File(assetsDir, assetName);
				if(asset.exists()) {
					return new FileInputStream(asset);
				} else {
					throw new FileNotFoundException(assetName);
				}
			}
		}
		throw new MalformedURLException(url.toExternalForm() + " is not a resource index");
	}
}
