package org.mcphackers.launchwrapper.protocol;

import static org.mcphackers.launchwrapper.protocol.URLStreamHandlerProxy.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.mcphackers.launchwrapper.protocol.SkinRequests.Profile;

public class TextureURLConnection extends HttpURLConnection {
    private SkinRequests skins;
    private InputStream inputStream;
    private HttpURLConnection directConnection;

    protected TextureURLConnection(URL u, SkinRequests skins) {
        super(u);
        this.skins = skins;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        if(inputStream != null) {
            return inputStream;
        }
        if(directConnection == null) {
            return null;
        }
        return directConnection.getInputStream();
    }


    @Override
    public void connect() throws IOException {
        responseCode = 200;
		String textureHash = url.getPath().substring("/texture/".length());
        Profile p = skins.getProfileFromTexture(textureHash);
        if(p != null) {
            if(textureHash.equals(p.skinTex)) {
                byte[] skin = skins.getSkin(p);
                if(skin != null) {
                    inputStream = new ByteArrayInputStream(skin);
                    return;
                }
            }
            else if(textureHash.equals(p.capeTex)) {
                byte[] cape = skins.getCape(p);
                if(cape != null) {
                    inputStream = new ByteArrayInputStream(cape);
                    return;
                }
            }
        }
        directConnection = (HttpURLConnection)openDirectConnection(url);
        directConnection.connect();
        responseCode = directConnection.getResponseCode();
    }

    @Override
    public void disconnect() {
        if(directConnection != null) {
            directConnection.disconnect();
        }
    }

    @Override
    public boolean usingProxy() {
        if(directConnection != null) {
            return directConnection.usingProxy();
        }
        return false;
    }
    
}
