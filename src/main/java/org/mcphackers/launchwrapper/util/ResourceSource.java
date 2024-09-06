package org.mcphackers.launchwrapper.util;

public interface ResourceSource {

	byte[] getResourceData(String path);

	void overrideResource(String path, byte[] data);

}
