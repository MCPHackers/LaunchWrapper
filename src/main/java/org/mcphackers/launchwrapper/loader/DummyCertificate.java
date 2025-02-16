package org.mcphackers.launchwrapper.loader;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;

public class DummyCertificate extends Certificate {
	public DummyCertificate() {
		super("dummy");
	}

	@Override
	public byte[] getEncoded() throws CertificateEncodingException {
		return null;
	}

	@Override
	public void verify(PublicKey key) throws CertificateException, NoSuchAlgorithmException, InvalidKeyException, NoSuchProviderException, SignatureException {
	}

	@Override
	public void verify(PublicKey key, String sigProvider) throws CertificateException, NoSuchAlgorithmException, InvalidKeyException, NoSuchProviderException, SignatureException {
	}

	@Override
	public String toString() {
		return "DummyCertificate";
	}

	@Override
	public PublicKey getPublicKey() {
		return null;
	}
}
