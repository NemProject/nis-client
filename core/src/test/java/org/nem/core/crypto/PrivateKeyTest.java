package org.nem.core.crypto;

import java.math.BigInteger;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.serialization.Deserializer;
import org.nem.core.test.Utils;

public class PrivateKeyTest {

	// region constructors / factories

	@Test
	public void canCreateFromBigInteger() {
		// Arrange:
		final PrivateKey key = new PrivateKey(new BigInteger("2275"));

		// Assert:
		MatcherAssert.assertThat(key.getRaw(), IsEqual.equalTo(new BigInteger("2275")));
	}

	@Test
	public void canCreateFromDecimalString() {
		// Arrange:
		final PrivateKey key = PrivateKey.fromDecimalString("2279");

		// Assert:
		MatcherAssert.assertThat(key.getRaw(), IsEqual.equalTo(new BigInteger("2279")));
	}

	@Test
	public void canCreateFromNegativeDecimalString() {
		// Arrange:
		final PrivateKey key = PrivateKey.fromDecimalString("-2279");

		// Assert:
		MatcherAssert.assertThat(key.getRaw(), IsEqual.equalTo(new BigInteger("-2279")));
	}

	@Test
	public void canCreateFromHexString() {
		// Arrange:
		final PrivateKey key = PrivateKey.fromHexString("227F");

		// Assert:
		MatcherAssert.assertThat(key.getRaw(), IsEqual.equalTo(new BigInteger("227F", 16)));
	}

	@Test
	public void canCreateFromOddLengthHexString() {
		// Arrange:
		final PrivateKey key = PrivateKey.fromHexString("ABC");

		// Assert:
		MatcherAssert.assertThat(key.getRaw(), IsEqual.equalTo(new BigInteger(new byte[]{
				(byte) 0x0A, (byte) 0xBC
		})));
	}

	@Test
	public void canCreateFromNegativeHexString() {
		// Arrange:
		final PrivateKey key = PrivateKey.fromHexString("8000");

		// Assert:
		MatcherAssert.assertThat(key.getRaw(), IsEqual.equalTo(new BigInteger(1, new byte[]{
				(byte) 0x80, 0x00
		})));
	}

	@Test(expected = CryptoException.class)
	public void cannotCreateAroundMalformedDecimalString() {
		// Act:
		PrivateKey.fromDecimalString("22A75");
	}

	@Test(expected = CryptoException.class)
	public void cannotCreateAroundMalformedHexString() {
		// Act:
		PrivateKey.fromHexString("22G75");
	}

	// endregion

	// region serializer

	@Test
	public void keyCanBeRoundTripped() {
		// Act:
		final PrivateKey key = createRoundTrippedKey(PrivateKey.fromHexString("A123E"));

		// Assert:
		MatcherAssert.assertThat(key, IsEqual.equalTo(PrivateKey.fromHexString("A123E")));
	}

	private static PrivateKey createRoundTrippedKey(final PrivateKey originalKey) {
		// Act:
		final Deserializer deserializer = Utils.roundtripSerializableEntity(originalKey, null);
		return new PrivateKey(deserializer);
	}

	// endregion

	// region equals / hashCode

	@Test
	public void equalsOnlyReturnsTrueForEquivalentObjects() {
		// Arrange:
		final PrivateKey key = new PrivateKey(new BigInteger("2275"));

		// Assert:
		MatcherAssert.assertThat(PrivateKey.fromDecimalString("2275"), IsEqual.equalTo(key));
		MatcherAssert.assertThat(PrivateKey.fromDecimalString("2276"), IsNot.not(IsEqual.equalTo(key)));
		MatcherAssert.assertThat(PrivateKey.fromHexString("2276"), IsNot.not(IsEqual.equalTo(key)));
		MatcherAssert.assertThat(null, IsNot.not(IsEqual.equalTo(key)));
		MatcherAssert.assertThat(new BigInteger("1235"), IsNot.not(IsEqual.equalTo((Object) key)));
	}

	@Test
	public void hashCodesAreEqualForEquivalentObjects() {
		// Arrange:
		final PrivateKey key = new PrivateKey(new BigInteger("2275"));
		final int hashCode = key.hashCode();

		// Assert:
		MatcherAssert.assertThat(PrivateKey.fromDecimalString("2275").hashCode(), IsEqual.equalTo(hashCode));
		MatcherAssert.assertThat(PrivateKey.fromDecimalString("2276").hashCode(), IsNot.not(IsEqual.equalTo(hashCode)));
		MatcherAssert.assertThat(PrivateKey.fromHexString("2275").hashCode(), IsNot.not(IsEqual.equalTo(hashCode)));
	}

	// endregion

	// region toString

	@Test
	public void toStringReturnsHexRepresentation() {
		// Assert:
		MatcherAssert.assertThat(PrivateKey.fromHexString("2275").toString(), IsEqual.equalTo("2275"));
		MatcherAssert.assertThat(PrivateKey.fromDecimalString("2275").toString(), IsEqual.equalTo("08e3"));
	}

	// endregion
}
