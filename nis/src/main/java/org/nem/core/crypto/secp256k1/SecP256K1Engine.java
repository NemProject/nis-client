package org.nem.core.crypto.secp256k1;

import org.nem.core.crypto.*;

/**
 * Class that wraps the SECP256K1 specific implementation.
 * TODO 20141010 J-B should this be called SecP256K1CRYPTOEngine?
 */
public class SecP256K1Engine implements CryptoEngines.CryptoEngine {

	@Override
	public Curve getCurve() {
		return SecP256K1Curve.secp256k1();
	}

	@Override
	public DsaSigner createDsaSigner(final KeyPair keyPair) {
		return new SecP256K1DsaSigner(keyPair);
	}

	@Override
	public KeyGenerator createKeyGenerator() {
		return new SecP256K1KeyGenerator();
	}

	@Override
	public BlockCipher createBlockCipher(final KeyPair senderKeyPair, final KeyPair recipientKeyPair) {
		return new SecP256K1BlockCipher(senderKeyPair, recipientKeyPair);
	}

	@Override
	public KeyAnalyzer createKeyAnalyzer() {
		return new SecP256K1KeyAnalyzer();
	}
}
