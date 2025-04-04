package org.mcphackers.launchwrapper.protocol;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import org.mcphackers.launchwrapper.Launch;
import org.mcphackers.launchwrapper.protocol.AssetRequests.AssetObject;

public class AssetURLConnection extends URLConnection {
	private final AssetRequests assets;

	public AssetURLConnection(URL url, AssetRequests assets) {
		super(url);
		this.assets = assets;
	}

	private InputStream getIndex(final boolean xml) throws IOException {
		StringBuilder s = new StringBuilder();
		if (xml) {
			s.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>");
			s.append("<ListBucketResult>");
			for (AssetObject asset : assets.list()) {
				if (!asset.path.contains("/")) {
					Launch.LOGGER.logDebug("Invalid resource: " + asset.path);
					continue;
				}
				s.append("<Contents>");
				s.append("<Key>");
				s.append(asset.path);
				s.append("</Key>");
				s.append("<Size>");
				s.append(Long.toString(asset.size));
				s.append("</Size>");
				s.append("</Contents>");
			}
			s.append("</ListBucketResult>");
		} else {
			for (AssetObject asset : assets.list()) {
				if (!asset.path.contains("/")) {
					Launch.LOGGER.logDebug("Invalid resource: " + asset.path);
					continue;
				}
				s.append(asset.path + ',' + asset.size + ",0\n");
			}
		}
		return new ByteArrayInputStream(s.toString().getBytes());
	}

	@Override
	public InputStream getInputStream() throws IOException {
		String key = url.getPath().replace("%20", " ").substring(1);
		key = key.substring(key.indexOf('/') + 1);
		if (key.length() == 0) {
			boolean xml = url.getPath().startsWith("/MinecraftResources/");
			return getIndex(xml);
		}
		File file = assets.getAssetFile(key);
		if (file == null) {
			throw new FileNotFoundException();
		}
		return new FileInputStream(file);
	}

	@Override
	public void connect() throws IOException {
	}
}
