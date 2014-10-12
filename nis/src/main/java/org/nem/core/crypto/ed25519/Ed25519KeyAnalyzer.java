package org.nem.core.crypto.ed25519;

import org.nem.core.crypto.*;

/**
 * Implementation of the key analyzer for Ed25519.
 */
public class Ed25519KeyAnalyzer implements KeyAnalyzer {
	private final static int COMPRESSED_KEY_SIZE = 32;

	@Override
	public boolean isKeyCompressed(final PublicKey publicKey) {
		if (COMPRESSED_KEY_SIZE != publicKey.getRaw().length) {
			return false;
		}

		// TODO 20141005 BR: more tests?
		return true;
	}
}