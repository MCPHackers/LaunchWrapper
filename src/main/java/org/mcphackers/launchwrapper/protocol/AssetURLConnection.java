package org.mcphackers.launchwrapper.protocol;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLConnection;

import org.mcphackers.launchwrapper.protocol.AssetRequests.AssetObject;

public class AssetURLConnection extends URLConnection {
	private AssetRequests assets;

	public AssetURLConnection(URL url, AssetRequests assets) {
		super(url);
		this.assets = assets;
	}

	@SuppressWarnings("resource")
	private InputStream getIndex(boolean xml) throws IOException {
		PipedInputStream in = new PipedInputStream();
		PrintWriter out = new PrintWriter(new PipedOutputStream(in), true);
		
		new Thread(() -> {
			if(xml) {
				out.write("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>");
				out.write("<ListBucketResult>");
			}
			for(AssetObject asset : assets.list()) {
				// path,size,last_updated_timestamp(unused)
				if(xml) {
					out.write("<Contents>");
					out.write("<Key>");
					out.write(asset.path);
					out.write("</Key>");
					out.write("<Size>");
					out.write(Long.toString(asset.size));
					out.write("</Size>");
					out.write("</Contents>");
				} else {
					out.write(asset.path + ',' + asset.size + ",0\n");
				}
			}
			if(xml) {
				out.write("</ListBucketResult>");
			}
			out.close();
		}).start();
		return in;
	}

	@Override
	public InputStream getInputStream() throws IOException {
		String key = url.getPath().replaceAll("%20", " ").substring(1);
		key = key.substring(key.indexOf('/') + 1);
		if(key.isEmpty()) {
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
