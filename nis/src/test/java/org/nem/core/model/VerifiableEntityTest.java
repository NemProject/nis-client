package org.nem.core.model;

import net.minidev.json.JSONObject;
import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.crypto.*;
import org.nem.core.serialization.*;
import org.nem.core.test.*;

import java.security.InvalidParameterException;

public class VerifiableEntityTest {

    //region Constructor

    @Test
    public void ctorCanCreateEntityForAccountWithSignerPrivateKey() {
        // Arrange:
        final KeyPair publicPrivateKeyPair = new KeyPair();
        final Account signer = new Account(publicPrivateKeyPair);

		// Act:
        final MockVerifiableEntity entity = new MockVerifiableEntity(signer, 6);

        // Assert:
        Assert.assertThat(entity.getType(), IsEqual.equalTo(MockVerifiableEntity.TYPE));
        Assert.assertThat(entity.getVersion(), IsEqual.equalTo(MockVerifiableEntity.VERSION));
        Assert.assertThat(entity.getCustomField(), IsEqual.equalTo(6));
        Assert.assertThat(entity.getSigner(), IsEqual.equalTo(signer));
		Assert.assertThat(entity.getSignature(), IsEqual.equalTo(null));
    }

    @Test
    public void ctorCanCreateEntityForAccountWithoutSignerPrivateKey() {
        // Arrange:
        final KeyPair publicPrivateKeyPair = new KeyPair();
        final KeyPair publicOnlyKeyPair = new KeyPair(publicPrivateKeyPair.getPublicKey());

        // Act:
        new MockVerifiableEntity(new Account(publicOnlyKeyPair));
    }

    @Test(expected = InvalidParameterException.class)
     public void ctorCannotCreateEntityForAccountWithoutSignerKeyPair() {
        // Arrange:
        final Address address = Address.fromEncoded("Alpha");

        // Act:
        new MockVerifiableEntity(new Account(address));
    }

    //endregion

    //region Serialization

    @Test
    public void verifiableEntityCanBeRoundTripped() {
        // Arrange:
        final Account signer = Utils.generateRandomAccount();
        final Account signerPublicKeyOnly = Utils.createPublicOnlyKeyAccount(signer);
        final MockVerifiableEntity originalEntity = new MockVerifiableEntity(signer, 7);
        final MockVerifiableEntity entity = createRoundTrippedEntity(originalEntity, signerPublicKeyOnly);

        // Assert:
        Assert.assertThat(entity.getType(), IsEqual.equalTo(MockVerifiableEntity.TYPE));
        Assert.assertThat(entity.getVersion(), IsEqual.equalTo(MockVerifiableEntity.VERSION));
        Assert.assertThat(entity.getCustomField(), IsEqual.equalTo(7));
        Assert.assertThat(entity.getSigner(), IsEqual.equalTo(signerPublicKeyOnly));
		Assert.assertThat(entity.getSignature(), IsNot.not(IsEqual.equalTo(null)));
    }

    @Test
    public void nonVerifiableEntityCanBeRoundTripped() {
        // Arrange:
        final Account signer = Utils.generateRandomAccount();
        final Account signerPublicKeyOnly = Utils.createPublicOnlyKeyAccount(signer);
        final MockVerifiableEntity originalEntity = new MockVerifiableEntity(signer, 7);
        final MockVerifiableEntity entity = createNonVerifiableRoundTrippedEntity(originalEntity, signerPublicKeyOnly);

        // Assert:
        Assert.assertThat(entity.getType(), IsEqual.equalTo(MockVerifiableEntity.TYPE));
        Assert.assertThat(entity.getVersion(), IsEqual.equalTo(MockVerifiableEntity.VERSION));
        Assert.assertThat(entity.getCustomField(), IsEqual.equalTo(7));
        Assert.assertThat(entity.getSigner(), IsEqual.equalTo(signerPublicKeyOnly));
        Assert.assertThat(entity.getSignature(), IsEqual.equalTo(null));
    }

    @Test
    public void verifiableRoundTrippedEntityCanBeVerified() {
        // Arrange:
        final Account signer = Utils.generateRandomAccount();
        final Account signerPublicKeyOnly = Utils.createPublicOnlyKeyAccount(signer);
        final MockVerifiableEntity entity = createRoundTrippedEntity(signer, 7, signerPublicKeyOnly);

        // Assert:
        Assert.assertThat(entity.verify(), IsEqual.equalTo(true));
    }

    @Test
    public void verifiableRoundTrippedEntityCanBeVerifiedWhenSignerAccountIsUnknown() {
        // Arrange:
        final Account signer = Utils.generateRandomAccount();
        final AccountLookup accountLookup = new MockAccountLookup(true);
        final MockVerifiableEntity entity = createRoundTrippedEntity(signer, 7, accountLookup);

        // Assert:
        Assert.assertThat(entity.verify(), IsEqual.equalTo(true));
    }

    @Test(expected = CryptoException.class)
    public void nonVerifiableRoundTrippedEntityCannotBeVerified() {
        // Arrange:
        final Account signer = Utils.generateRandomAccount();
        final Account signerPublicKeyOnly = Utils.createPublicOnlyKeyAccount(signer);
        final MockVerifiableEntity originalEntity = new MockVerifiableEntity(signer);
        final MockVerifiableEntity entity = createNonVerifiableRoundTrippedEntity(originalEntity, signerPublicKeyOnly);

        // Assert:
        entity.verify();
    }

    @Test(expected = SerializationException.class)
    public void verifiableSerializationRequiresSignature() {
        // Arrange:
        final Account signer = Utils.generateRandomAccount();
        final MockVerifiableEntity entity = new MockVerifiableEntity(signer);

        // Act:
        entity.serialize(new JsonSerializer());
    }

    @Test
    public void nonVerifiableSerializationDoesNotRequireSignature() {
        // Arrange:
        final Account signer = Utils.generateRandomAccount();
        final MockVerifiableEntity entity = new MockVerifiableEntity(signer);

        // Act:
        entity.asNonVerifiable().serialize(new JsonSerializer());
    }

    @Test
    public void verifiableSerializationIncludesSignature() {
        // Arrange:
        final Account signer = Utils.generateRandomAccount();
        final MockVerifiableEntity entity = new MockVerifiableEntity(signer);
        final JsonSerializer serializer = new JsonSerializer();

        // Act:
        entity.sign();
        entity.serialize(serializer);
        final JSONObject object = serializer.getObject();

        // Assert:
        Assert.assertThat(object.containsKey("signature"), IsEqual.equalTo(true));
    }

    @Test
    public void nonVerifiableSerializationExcludesSignature() {
        // Arrange:
        final Account signer = Utils.generateRandomAccount();
        final MockVerifiableEntity entity = new MockVerifiableEntity(signer);
        final JsonSerializer serializer = new JsonSerializer();

        // Act:
        entity.sign();
        entity.asNonVerifiable().serialize(serializer);
        final JSONObject object = serializer.getObject();

        // Assert:
        Assert.assertThat(object.containsKey("signature"), IsEqual.equalTo(false));
    }

    //endregion

    //region Sign / Verify

    @Test
    public void signCreatesValidSignature() {
        // Arrange:
        final Account signer = Utils.generateRandomAccount();
        final MockVerifiableEntity entity = new MockVerifiableEntity(signer);

        // Act:
        entity.sign();

        // Assert:
        Assert.assertThat(entity.getSignature(), IsNot.not(IsEqual.equalTo(null)));
        Assert.assertThat(entity.verify(), IsEqual.equalTo(true));
    }

    @Test
    public void changingFieldInvalidatesSignature() {
        // Arrange:
        final Account signer = Utils.generateRandomAccount();
        final MockVerifiableEntity entity = new MockVerifiableEntity(signer, 7);

        // Act:
        entity.sign();
        entity.setCustomField(12);

        // Assert:
        Assert.assertThat(entity.getSignature(), IsNot.not(IsEqual.equalTo(null)));
        Assert.assertThat(entity.verify(), IsEqual.equalTo(false));
    }

    @Test(expected = InvalidParameterException.class)
    public void cannotSignWithoutPrivateKey() {
        // Arrange:
        final Account signer = Utils.generateRandomAccount();
        final Account signerPublicKeyOnly = Utils.createPublicOnlyKeyAccount(signer);
        final MockVerifiableEntity entity = createRoundTrippedEntity(signer, 7, signerPublicKeyOnly);

        // Assert:
        entity.sign();
    }

    @Test(expected = CryptoException.class)
    public void cannotVerifyWithoutSignature() {
        // Arrange:
        final Account signer = Utils.generateRandomAccount();
        final MockVerifiableEntity entity = new MockVerifiableEntity(signer, 7);

        // Act:
        entity.verify();
    }

    //endregion

    //region External Signature

    @Test
    public void signatureCanBeSetExternally() {
        // Arrange:
        final Account signer = Utils.generateRandomAccount();
        final MockVerifiableEntity entity = new MockVerifiableEntity(signer, 7);
        final Signature signature = new Signature(Utils.generateRandomBytes(64));

        // Act:
        entity.setSignature(signature);

        // Assert:
        Assert.assertThat(entity.getSignature(), IsEqual.equalTo(signature));
    }

    @Test
    public void nonMatchingExternalSignatureCannotBeVerified() {
        // Arrange:
        final Account signer = Utils.generateRandomAccount();
        final MockVerifiableEntity entity = new MockVerifiableEntity(signer, 7);

        // Act:
        entity.setSignature(new Signature(Utils.generateRandomBytes(64)));

        // Assert:
        Assert.assertThat(entity.verify(), IsEqual.equalTo(false));
    }

    @Test
    public void matchingExternalSignatureCanBeVerified() {
        // Arrange:
        final Account signer = Utils.generateRandomAccount();
        final MockVerifiableEntity entity1 = new MockVerifiableEntity(signer, 7);
        final MockVerifiableEntity entity2 = new MockVerifiableEntity(signer, 7);

        // Act:
        entity1.sign();
        entity2.setSignature(entity1.getSignature());

        // Assert:
        Assert.assertThat(entity2.verify(), IsEqual.equalTo(true));
    }

    //endregion

    private static MockVerifiableEntity createRoundTrippedEntity(
        final Account originalSigner,
        final int customField,
        final Account deserializedSigner) {
        // Act:
        final MockVerifiableEntity originalEntity = new MockVerifiableEntity(originalSigner, customField);
        return createRoundTrippedEntity(originalEntity, deserializedSigner);
    }

    private static MockVerifiableEntity createRoundTrippedEntity(
        final MockVerifiableEntity originalEntity,
        final Account deserializedSigner) {
        // Act:
        Deserializer deserializer = Utils.roundtripVerifiableEntity(originalEntity, deserializedSigner);
        return new MockVerifiableEntity(deserializer);
    }

    private static MockVerifiableEntity createRoundTrippedEntity(
        final Account originalSigner,
        final int customField,
        final AccountLookup accountLookup) {
        // Arrange:
        final MockVerifiableEntity originalEntity = new MockVerifiableEntity(originalSigner, customField);
        originalEntity.sign();

        // Act:
        Deserializer deserializer = Utils.roundtripSerializableEntity(originalEntity, accountLookup);
        return new MockVerifiableEntity(deserializer);
    }

    private static MockVerifiableEntity createNonVerifiableRoundTrippedEntity(
        final MockVerifiableEntity originalEntity,
        final Account deserializedSigner) {
        // Arrange:
        final MockAccountLookup accountLookup = new MockAccountLookup();
        accountLookup.setMockAccount(deserializedSigner);

        // Act:
        Deserializer deserializer = Utils.roundtripSerializableEntity(originalEntity.asNonVerifiable(), accountLookup);
        return new MockVerifiableEntity(VerifiableEntity.DeserializationOptions.NON_VERIFIABLE, deserializer);
    }
}