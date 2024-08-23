package org.mcphackers.launchwrapper.protocol;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import org.mcphackers.launchwrapper.protocol.AssetRequests.AssetObject;

public class AssetURLConnection extends URLConnection {
	private AssetRequests assets;

	public AssetURLConnection(URL url, AssetRequests assets) {
		super(url);
		this.assets = assets;
	}

	private InputStream getIndex(final boolean xml) throws IOException {
		StringBuilder s = new StringBuilder();
		if(xml) {
			s.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>");
			s.append("<ListBucketResult>");
		}
		for(AssetObject asset : assets.list()) {
			// path,size,last_updated_timestamp(unused)
			if(xml) {
				s.append("<Contents>");
				s.append("<Key>");
				s.append(asset.path);
				s.append("</Key>");
				s.append("<Size>");
				s.append(Long.toString(asset.size));
				s.append("</Size>");
				s.append("</Contents>");
			} else {
				s.append(asset.path + ',' + asset.size + ",0\n");
			}
		}
		if(xml) {
			s.append("</ListBucketResult>");
		}
		return new ByteArrayInputStream(s.toString().getBytes());
	}

	@Override
	public InputStream getInputStream() throws IOException {
		String key = url.getPath().replace("%20", " ").substring(1);
		key = key.substring(key.indexOf('/') + 1);
		if(key.length() == 0) {
			boolean xml = url.getPath().startsWith("/MinecraftResources/");
			return getIndex(xml);
		}
		AssetObject object = assets.get(key);
		if(object != null) {
			return new FileInputStream(object.file);
		}
		throw new FileNotFoundException();
	}

	@Override
	public void connect() throws IOException {
	}
}