package org.nem.core.crypto;

import net.minidev.json.JSONObject;
import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.serialization.*;
import org.nem.core.test.Utils;
import org.nem.core.utils.Base64Encoder;

import java.math.BigInteger;

public class SignatureTest {

	//region constructor

	@Test
	public void bigIntegerCtorInitializesFields() {
		// Arrange:
		BigInteger r = new BigInteger("99512345");
		BigInteger s = new BigInteger("12351234");

		// Act:
		Signature signature = new Signature(r, s);

		// Assert:
		Assert.assertThat(signature.getR(), IsEqual.equalTo(r));
		Assert.assertThat(signature.getS(), IsEqual.equalTo(s));
	}

	@Test
	public void byteArrayCtorInitializesFields() {
		// Arrange:
		final Signature originalSignature = createSignature("99512345", "12351234");

		// Act:
		Signature signature = new Signature(originalSignature.getBytes());

		// Assert:
		Assert.assertThat(signature.getR(), IsEqual.equalTo(originalSignature.getR()));
		Assert.assertThat(signature.getS(), IsEqual.equalTo(originalSignature.getS()));
	}

	@Test(expected = IllegalArgumentException.class)
	public void byteArrayCtorFailsIfByteArrayIsTooSmall() {
		// Act:
		new Signature(new byte[63]);
	}

	@Test(expected = IllegalArgumentException.class)
	public void byteArrayCtorFailsIfByteArrayIsTooLarge() {
		// Act:
		new Signature(new byte[65]);
	}

	@Test
	public void byteArrayCtorSucceedsIfByteArrayIsCorrectLength() {
		// Act:
		Signature signature = new Signature(new byte[64]);

		// Assert:
		Assert.assertThat(signature.getR(), IsEqual.equalTo(BigInteger.ZERO));
		Assert.assertThat(signature.getS(), IsEqual.equalTo(BigInteger.ZERO));
	}

	//endregion

	//region isCanonical / makeCanonical

	@Test
	public void isCanonicalReturnsTrueForCanonicalSignature() {
		// Arrange:
		Signature signature = createCanonicalSignature();

		// Assert:
		Assert.assertThat(signature.isCanonical(), IsEqual.equalTo(true));
	}

	@Test
	public void isCanonicalReturnsFalseForNonCanonicalSignature() {
		// Arrange:
		Signature signature = makeNonCanonical(createCanonicalSignature());

		// Assert:
		Assert.assertThat(signature.isCanonical(), IsEqual.equalTo(false));
	}

	@Test
	public void makeCanonicalMakesNonCanonicalSignatureCanonical() {
		// Arrange:
		Signature signature = createCanonicalSignature();
		Signature nonCanonicalSignature = makeNonCanonical(signature);

		Assert.assertThat(nonCanonicalSignature.isCanonical(), IsEqual.equalTo(false));

		// Act:
		nonCanonicalSignature.makeCanonical();

		// Assert:
		Assert.assertThat(nonCanonicalSignature.isCanonical(), IsEqual.equalTo(true));
		Assert.assertThat(nonCanonicalSignature.getR(), IsEqual.equalTo(signature.getR()));
		Assert.assertThat(nonCanonicalSignature.getS(), IsEqual.equalTo(signature.getS()));
	}

	//endregion

	//region getBytes

	@Test
	public void getBytesReturns64Bytes() {
		// Assert:
		for (final Signature signature : createRoundtripTestSignatures())
			Assert.assertThat(signature.getBytes().length, IsEqual.equalTo(64));
	}

	@Test
	public void canRoundtripBinarySignature() {
		// Assert:
		for (final Signature signature : createRoundtripTestSignatures())
			Assert.assertThat(new Signature(signature.getBytes()), IsEqual.equalTo(signature));
	}

	private Signature[] createRoundtripTestSignatures() {
		return new Signature[] {
				createSignature(Utils.createString('F', 64), Utils.createString('0', 64)),
				createSignature(Utils.createString('0', 64), Utils.createString('F', 64)),
				createSignature("99512345", "12351234")
		};
	}

	//endregion

	//region equals / hashCode

	@Test
	public void equalsOnlyReturnsTrueForEquivalentObjects() {
		// Arrange:
		Signature signature = createSignature(1235, 7789);

		// Assert:
		Assert.assertThat(createSignature(1235, 7789), IsEqual.equalTo(signature));
		Assert.assertThat(createSignature(1234, 7789), IsNot.not(IsEqual.equalTo(signature)));
		Assert.assertThat(createSignature(1235, 7790), IsNot.not(IsEqual.equalTo(signature)));
		Assert.assertThat(null, IsNot.not(IsEqual.equalTo(signature)));
		Assert.assertThat(new BigInteger("1235"), IsNot.not(IsEqual.equalTo((Object)signature)));
	}

	@Test
	public void hashCodesAreEqualForEquivalentObjects() {
		// Arrange:
		Signature signature = createSignature(1235, 7789);
		int hashCode = signature.hashCode();

		// Assert:
		Assert.assertThat(createSignature(1235, 7789).hashCode(), IsEqual.equalTo(hashCode));
		Assert.assertThat(createSignature(1234, 7789).hashCode(), IsNot.not(IsEqual.equalTo(hashCode)));
		Assert.assertThat(createSignature(1235, 7790).hashCode(), IsNot.not(IsEqual.equalTo(hashCode)));
	}

	//endregion

	//region inline serialization

	@Test
	public void canWriteSignature() {
		// Arrange:
		final JsonSerializer serializer = new JsonSerializer();
		final Signature signature = new Signature(new BigInteger("7A", 16), new BigInteger("A4F0", 16));

		// Act:
		Signature.writeTo(serializer, "Signature", signature);

		// Assert:
		final JSONObject object = serializer.getObject();
		Assert.assertThat(object.size(), IsEqual.equalTo(1));
		Assert.assertThat((String)object.get("Signature"), IsEqual.equalTo(Base64Encoder.getString(signature.getBytes())));
	}

	@Test
	public void canRoundtripSignature() {
		// Arrange:
		final JsonSerializer serializer = new JsonSerializer();
		final Signature originalSignature = new Signature(new BigInteger("7A", 16), new BigInteger("A4F0", 16));

		// Act:
		Signature.writeTo(serializer, "Signature", originalSignature);

		final JsonDeserializer deserializer = Utils.createDeserializer(serializer.getObject());
		final Signature signature = Signature.readFrom(deserializer, "Signature");

		// Assert:
		Assert.assertThat(signature, IsEqual.equalTo(originalSignature));
	}

	//endregion

	private static Signature createSignature(final String r, final String s) {
		return new Signature(new BigInteger(r, 16), new BigInteger(s, 16));
	}

	private static Signature createSignature(final int r, final int s) {
		return new Signature(new BigInteger(String.format("%d", r)), new BigInteger(String.format("%d", s)));
	}

	private static Signature createCanonicalSignature() {
		// Arrange:
		KeyPair kp = new KeyPair();
		Signer signer = new Signer(kp);
		byte[] input = Utils.generateRandomBytes();

		// Act:
		return signer.sign(input);
	}

	private static Signature makeNonCanonical(Signature signature) {
		// Act:
		BigInteger nonCanonicalS = Curves.secp256k1().getParams().getN().subtract(signature.getS());
		return new Signature(signature.getR(), nonCanonicalS);
	}
}
