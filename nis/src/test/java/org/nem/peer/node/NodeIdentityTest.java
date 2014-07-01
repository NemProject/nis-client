package org.nem.peer.node;

import net.minidev.json.JSONObject;
import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.crypto.*;
import org.nem.core.serialization.*;
import org.nem.core.test.Utils;
import org.nem.core.utils.ArrayUtils;

public class NodeIdentityTest {

	//region constructor

	@Test
	public void identityCanBeCreatedAroundPublicKey() {
		// Arrange:
		final KeyPair keyPair = new KeyPair(Utils.generateRandomPublicKey());

		// Act:
		final NodeIdentity identity = new NodeIdentity(keyPair);

		// Assert:
		Assert.assertThat(identity.getKeyPair(), IsSame.sameInstance(keyPair));
		Assert.assertThat(identity.getAddress().getPublicKey(), IsEqual.equalTo(keyPair.getPublicKey()));
		Assert.assertThat(identity.isOwned(), IsEqual.equalTo(false));
		Assert.assertThat(identity.getName(), IsNull.nullValue());
	}

	@Test
	public void identityCanBeCreatedAroundPrivateKey() {
		// Arrange:
		final KeyPair keyPair = new KeyPair();

		// Act:
		final NodeIdentity identity = new NodeIdentity(keyPair);

		// Assert:
		Assert.assertThat(identity.getKeyPair(), IsSame.sameInstance(keyPair));
		Assert.assertThat(identity.getAddress().getPublicKey(), IsEqual.equalTo(keyPair.getPublicKey()));
		Assert.assertThat(identity.isOwned(), IsEqual.equalTo(true));
		Assert.assertThat(identity.getName(), IsNull.nullValue());
	}

	@Test
	public void identityCanBeCreatedWithFriendlyName() {
		// Arrange:
		final KeyPair keyPair = new KeyPair();

		// Act:
		final NodeIdentity identity = new NodeIdentity(keyPair, "bob");

		// Assert:
		Assert.assertThat(identity.getKeyPair(), IsSame.sameInstance(keyPair));
		Assert.assertThat(identity.getAddress().getPublicKey(), IsEqual.equalTo(keyPair.getPublicKey()));
		Assert.assertThat(identity.isOwned(), IsEqual.equalTo(true));
		Assert.assertThat(identity.getName(), IsEqual.equalTo("bob"));
	}

	//endregion

	//region sign

	@Test
	public void equalIdentitiesProduceSameSignatures() {
		// Arrange:
		final KeyPair keyPair = new KeyPair();
		final NodeIdentity identity1 = new NodeIdentity(keyPair);
		final NodeIdentity identity2 = new NodeIdentity(keyPair);

		// Act:
		final byte[] salt = Utils.generateRandomBytes();
		final Signature signature1 = identity1.sign(salt);
		final Signature signature2 = identity2.sign(salt);

		// Assert:
		Assert.assertThat(signature2, IsEqual.equalTo(signature1));
	}

	@Test
	public void equalIdentitiesWithDifferentSaltsProduceDifferentSignatures() {
		// Arrange:
		final KeyPair keyPair = new KeyPair();
		final NodeIdentity identity1 = new NodeIdentity(keyPair);
		final NodeIdentity identity2 = new NodeIdentity(keyPair);

		// Act:
		final Signature signature1 = identity1.sign(Utils.generateRandomBytes());
		final Signature signature2 = identity2.sign(Utils.generateRandomBytes());

		// Assert:
		Assert.assertThat(signature2, IsNot.not(IsEqual.equalTo(signature1)));
	}

	@Test
	public void differentIdentitiesProduceDifferentSignatures() {
		// Arrange:
		final NodeIdentity identity1 = new NodeIdentity(new KeyPair());
		final NodeIdentity identity2 = new NodeIdentity(new KeyPair());

		// Act:
		final byte[] salt = Utils.generateRandomBytes();
		final Signature signature1 = identity1.sign(salt);
		final Signature signature2 = identity2.sign(salt);

		// Assert:
		Assert.assertThat(signature2, IsNot.not(IsEqual.equalTo(signature1)));
	}

	@Test(expected = CryptoException.class)
	public void identityCannotSignSaltWithoutPrivateKey() {
		// Arrange:
		final NodeIdentity identity = new NodeIdentity(new KeyPair(Utils.generateRandomPublicKey()));

		// Act:
		final byte[] salt = Utils.generateRandomBytes();
		identity.sign(salt);
	}

	//endregion

	//region verify

	@Test
	public void signatureCanBeVerifiedByEqualIdentityWithoutPrivateKey() {
		// Arrange:
		final KeyPair keyPair = new KeyPair();
		final NodeIdentity identity1 = new NodeIdentity(keyPair);
		final NodeIdentity identity2 = new NodeIdentity(new KeyPair(keyPair.getPublicKey()));

		// Act:
		final byte[] salt = Utils.generateRandomBytes();
		final Signature signature = identity1.sign(salt);
		final boolean isVerified = identity2.verify(salt, signature);

		// Assert:
		Assert.assertThat(isVerified, IsEqual.equalTo(true));
	}

	@Test
	public void signatureCannotBeVerifiedBySameIdentityWithDifferentSalt() {
		// Arrange:
		final KeyPair keyPair = new KeyPair();
		final NodeIdentity identity1 = new NodeIdentity(keyPair);
		final NodeIdentity identity2 = new NodeIdentity(keyPair);

		// Act:
		final Signature signature = identity1.sign(Utils.generateRandomBytes());
		final boolean isVerified = identity2.verify(Utils.generateRandomBytes(), signature);

		// Assert:
		Assert.assertThat(isVerified, IsEqual.equalTo(false));
	}

	@Test
	public void signatureCannotBeVerifiedByDifferentIdentity() {
		// Arrange:
		final NodeIdentity identity1 = new NodeIdentity(new KeyPair());
		final NodeIdentity identity2 = new NodeIdentity(new KeyPair());

		// Act:
		final byte[] salt = Utils.generateRandomBytes();
		final Signature signature = identity1.sign(salt);
		final boolean isVerified = identity2.verify(salt, signature);

		// Assert:
		Assert.assertThat(isVerified, IsEqual.equalTo(false));
	}

	@Test
	public void signatureCannotBeVerifiedWithoutChallengePrefix() {
		// Arrange:
		final KeyPair keyPair = new KeyPair();
		final NodeIdentity identity = new NodeIdentity(keyPair);

		final byte[] payload = "alice is bad".getBytes();
		final byte[] challengePrefix = "nem trust challenge:".getBytes();
		final byte[] publicKey = keyPair.getPublicKey().getRaw();
		final byte[] payloadWithPrefix = ArrayUtils.concat(challengePrefix, publicKey, payload);

		final Signer signer = new Signer(keyPair);
		final Signature signedDataWithPrefix = signer.sign(payloadWithPrefix);
		final Signature signedDataWithoutPrefix = signer.sign(payload);

		// Assert:
		final NodeChallenge challenge = new NodeChallenge(payload);
		Assert.assertThat(identity.verify(challenge.getRaw(), signedDataWithPrefix), IsEqual.equalTo(true));
		Assert.assertThat(identity.verify(challenge.getRaw(), signedDataWithoutPrefix), IsEqual.equalTo(false));
	}

	//endregion

	//region serialization

	@Test
	public void identityWithPublicKeyCanBeRoundTripped() {
		// Arrange:
		final PublicKey publicKey = Utils.generateRandomPublicKey();

		// Act:
		final NodeIdentity identity = createRoundTrippedIdentity(new NodeIdentity(new KeyPair(publicKey), "alice"));

		// Assert:
		Assert.assertThat(identity.getAddress().getPublicKey(), IsEqual.equalTo(publicKey));
		Assert.assertThat(identity.getKeyPair().getPublicKey(), IsEqual.equalTo(publicKey));
		Assert.assertThat(identity.getKeyPair().getPrivateKey(), IsNull.nullValue());
		Assert.assertThat(identity.isOwned(), IsEqual.equalTo(false));
		Assert.assertThat(identity.getName(), IsEqual.equalTo("alice"));
	}

	@Test
	public void identityWithPrivateKeyCanBeRoundTrippedWithoutPrivateKey() {
		// Arrange:
		final KeyPair keyPair = new KeyPair();

		// ActL
		final NodeIdentity identity = createRoundTrippedIdentity(new NodeIdentity(keyPair, "bob"));

		// Assert:
		Assert.assertThat(identity.getAddress().getPublicKey(), IsEqual.equalTo(keyPair.getPublicKey()));
		Assert.assertThat(identity.getKeyPair().getPublicKey(), IsEqual.equalTo(keyPair.getPublicKey()));
		Assert.assertThat(identity.getKeyPair().getPrivateKey(), IsNull.nullValue());
		Assert.assertThat(identity.isOwned(), IsEqual.equalTo(false));
		Assert.assertThat(identity.getName(), IsEqual.equalTo("bob"));
	}

	@Test
	public void serializerPayloadDoesNotContainPrivateKey() {
		// Arrange:
		final KeyPair keyPair = new KeyPair();
		final NodeIdentity identity = new NodeIdentity(keyPair);
		final JsonSerializer serializer = new JsonSerializer();

		// Act:
		identity.serialize(serializer);
		final JSONObject jsonObject = serializer.getObject();

		// Assert:
		Assert.assertThat(jsonObject.size(), IsEqual.equalTo(2));
		Assert.assertThat(jsonObject.containsKey("public-key"), IsEqual.equalTo(true));
		Assert.assertThat(jsonObject.containsKey("name"), IsEqual.equalTo(true));
	}

	@Test
	public void jsonContainingPrivateKeyCanBeDeserialized() {
		// Arrange:
		final KeyPair keyPair = new KeyPair();
		final JsonSerializer serializer = new JsonSerializer(true);
		serializer.writeBigInteger("private-key", keyPair.getPrivateKey().getRaw());
		serializer.writeString("name", "trudy");

		// Act:
		final Deserializer deserializer = new JsonDeserializer(serializer.getObject(), null);
		final NodeIdentity identity = NodeIdentity.deserializeWithPrivateKey(deserializer);

		// Assert:
		Assert.assertThat(identity.getAddress().getPublicKey(), IsEqual.equalTo(keyPair.getPublicKey()));
		Assert.assertThat(identity.getKeyPair().getPublicKey(), IsEqual.equalTo(keyPair.getPublicKey()));
		Assert.assertThat(identity.getKeyPair().getPrivateKey(), IsEqual.equalTo(keyPair.getPrivateKey()));
		Assert.assertThat(identity.isOwned(), IsEqual.equalTo(true));
		Assert.assertThat(identity.getName(), IsEqual.equalTo("trudy"));
	}

	private static NodeIdentity createRoundTrippedIdentity(final NodeIdentity originalIdentity) {
		return NodeIdentity.deserializeWithPublicKey(Utils.roundtripSerializableEntity(originalIdentity, null)) ;
	}

	//endregion

	//region equals / hashCode

	@Test
	public void equalsOnlyReturnsTrueForEquivalentObjects() {
		// Arrange:
		final KeyPair keyPair = new KeyPair();
		final NodeIdentity identity = new NodeIdentity(keyPair);

		// Assert:
		Assert.assertThat(new NodeIdentity(keyPair), IsEqual.equalTo(identity));
		Assert.assertThat(new NodeIdentity(new KeyPair(keyPair.getPublicKey())), IsEqual.equalTo(identity));
		Assert.assertThat(new NodeIdentity(new KeyPair()), IsNot.not(IsEqual.equalTo(identity)));
		Assert.assertThat(null, IsNot.not(IsEqual.equalTo(identity)));
		Assert.assertThat(keyPair, IsNot.not(IsEqual.equalTo((Object)identity)));
	}

	@Test
	public void hashCodesAreEqualForEquivalentObjects() {
		// Arrange:
		final KeyPair keyPair = new KeyPair();
		final NodeIdentity identity = new NodeIdentity(keyPair);
		int hashCode = identity.hashCode();

		// Assert:
		Assert.assertThat(new NodeIdentity(keyPair).hashCode(), IsEqual.equalTo(hashCode));
		Assert.assertThat(new NodeIdentity(new KeyPair(keyPair.getPublicKey())).hashCode(), IsEqual.equalTo(hashCode));
		Assert.assertThat(new NodeIdentity(new KeyPair()).hashCode(), IsNot.not(IsEqual.equalTo(hashCode)));
	}

	//endregion

	//region toString

	private final PublicKey PUBLIC_KEY_FOR_TO_STRING_TESTS =
			PublicKey.fromHexString("02d3da82b0a291ba2cb1469c7e3bd65c255b797586acb774a66fff53f02ef509e9");

	@Test
	public void toStringReturnsAppropriateRepresentationWhenNameIsPresent() {
		// Arrange:
		final KeyPair keyPair = new KeyPair(PUBLIC_KEY_FOR_TO_STRING_TESTS);
		final NodeIdentity identity = new NodeIdentity(keyPair, "bob");

		// Assert:
		Assert.assertThat(identity.toString(), IsEqual.equalTo("bob <TDXJDTROHFWTJQDGFVFJBFMFXSDAPTBVJEQRVJEH>"));
	}

	@Test
	public void toStringReturnsAppropriateRepresentationWhenNameIsNotPresent() {
		// Arrange:
		final KeyPair keyPair = new KeyPair(PUBLIC_KEY_FOR_TO_STRING_TESTS);
		final NodeIdentity identity = new NodeIdentity(keyPair);

		// Assert:
		Assert.assertThat(identity.toString(), IsEqual.equalTo("<TDXJDTROHFWTJQDGFVFJBFMFXSDAPTBVJEQRVJEH>"));
	}

	//endregion
}