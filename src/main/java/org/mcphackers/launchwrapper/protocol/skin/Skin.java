package org.mcphackers.launchwrapper.protocol.skin;

import java.io.IOException;

public interface Skin {
	/**
	 * Optional. Return null if unavailable
	 * Used to get cached skin.
	 * @return sha256 hash string
	 */
	String getSHA256();

	/**
	 * @return skin data
	 */
	byte[] getData() throws IOException;

	/**
	 * @return a file:// or an http[s]:// url
	 */
	String getURL();

	/**
	 * Whenever skin is meant to be applied to a slim model
	 * The return value is ignored for CAPE and ELYTRA
	 */
	boolean isSlim();
}
