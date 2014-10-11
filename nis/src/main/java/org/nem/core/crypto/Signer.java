package org.nem.core.crypto;

/**
 * Wraps DSA signing and verification logic.
 */
public class Signer {

	private final DsaSigner signer;

	/**
	 * Creates a signer around a KeyPair.
	 *
	 * @param keyPair The KeyPair that should be used for signing and verification.
	 */
	public Signer(final KeyPair keyPair) {
		this.signer = CryptoEngines.getDefaultEngine().createDsaSigner(keyPair);
	}

	/**
	 * Signs the SHA3 hash of an arbitrarily sized message.
	 *
	 * @param data The message to sign.
	 * @return The generated signature.
	 */
	public Signature sign(final byte[] data) {
		return this.signer.sign(data);
	}

	/**
	 * Verifies that the signature is valid.
	 *
	 * @param data The original message.
	 * @param signature The generated signature.
	 * @return true if the signature is valid.
	 */
	public boolean verify(final byte[] data, final Signature signature) {
		return this.signer.verify(data, signature);
	}

	/**
	 * Determines if the given signature is canonical.
	 *
	 * @return true if the given signature is canonical.
	 */
	public boolean isCanonicalSignature(final Signature signature) {
		return this.signer.isCanonicalSignature(signature);
	}

	/**
	 * Makes this signature canonical.
	 */
	public Signature makeSignatureCanonical(final Signature signature) {
		return this.signer.makeSignatureCanonical(signature);
	}
}
