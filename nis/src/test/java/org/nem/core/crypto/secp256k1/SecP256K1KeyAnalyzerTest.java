package org.nem.core.crypto.secp256k1;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.crypto.*;
import org.nem.core.test.Utils;

public class SecP256K1KeyAnalyzerTest extends KeyAnalyzerTest {

	@Test
	public void compressedKeyMustHaveCorrectFirstByte() {
		// Arrange:
		final PublicKey publicKey = Utils.generateRandomPublicKey();
		final KeyAnalyzer analyzer = this.getKeyAnalyzer();

		// Assert:
		Assert.assertThat(analyzer.isKeyCompressed(this.createKeyWithFirstByte(publicKey, (byte)1)), IsEqual.equalTo(false));
		Assert.assertThat(analyzer.isKeyCompressed(this.createKeyWithFirstByte(publicKey, (byte)2)), IsEqual.equalTo(true));
		Assert.assertThat(analyzer.isKeyCompressed(this.createKeyWithFirstByte(publicKey, (byte)3)), IsEqual.equalTo(true));
		Assert.assertThat(analyzer.isKeyCompressed(this.createKeyWithFirstByte(publicKey, (byte)4)), IsEqual.equalTo(false));
	}

	private PublicKey createKeyWithFirstByte(final PublicKey key, final byte firstByte) {
		// Arrange:
		final byte[] modifiedPublicKey = new byte[key.getRaw().length];
		System.arraycopy(key.getRaw(), 0, modifiedPublicKey, 0, modifiedPublicKey.length);
		modifiedPublicKey[0] = firstByte;
		return new PublicKey(modifiedPublicKey);
	}

	@Override
	protected KeyAnalyzer getKeyAnalyzer() {
		return new SecP256K1KeyAnalyzer();
	}

	@Override
	@Before
	public void initCryptoEngine() {
		CryptoEngines.setDefaultEngine(CryptoEngines.secp256k1Engine());
	}
}