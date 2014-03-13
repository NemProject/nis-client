package org.nem.core.model;

import org.nem.core.crypto.*;
import org.nem.core.serialization.*;

import java.security.InvalidParameterException;

/**
 * Base class for all entities that need to be verified
 * (e.g. blocks and transactions).
 */
public abstract class VerifiableEntity implements SerializableEntity {

    /**
     * Enumeration of deserialization options.
     */
    public enum DeserializationOptions {
        /**
         * The serialized data includes a signature and is verifiable.
         */
        VERIFIABLE,

        /**
         * The serialized data does not include a signature and is not verifiable.
         */
        NON_VERIFIABLE
    }

    private final int version;
    private final int type;
    private final Account signer;
    private int timestamp;
    private Signature signature;

    //region Constructors

    /**
     * Creates a new verifiable entity.
     *
     * @param type The entity type.
     * @param version The entity version.
     * @param timestamp The entity timestamp.
     * @param signer The entity signer.
     */
    public VerifiableEntity(final int type, final int version, final int timestamp, final Account signer) {
        if (null == signer.getKeyPair())
            throw new InvalidParameterException("signer key pair is required to create a verifiable entity ");

        this.type = type;
        this.version = version;
        this.timestamp = timestamp;
        this.signer = signer;
    }

    /**
     * Deserializes a new transaction.
     *
     * @param type The transaction type.
     * @param options Deserialization options.
     * @param deserializer The deserializer to use.
     */
    public VerifiableEntity(final int type, DeserializationOptions options,  Deserializer deserializer) {
        this.type = type;
        this.version = deserializer.readInt("version");
        this.timestamp = deserializer.readInt("timestamp");
        this.signer = SerializationUtils.readAccount(deserializer, "signer", AccountEncoding.PUBLIC_KEY);

        if (DeserializationOptions.VERIFIABLE == options)
            this.signature = SerializationUtils.readSignature(deserializer, "signature");
    }

    //endregion

    //region Getters and Setters

    /**
     * Gets the version.
     *
     * @return The version.
     */
    public int getVersion() { return this.version; }

    /**
     * Gets the type.
     *
     * @return The type.
     */
    public int getType() { return this.type; }

    /**
     * Gets the signer.
     *
     * @return The signer.
     */
    public Account getSigner() { return this.signer; }

    /**
     * Gets the timestamp.
     *
     * @return The timestamp.
     */
    public int getTimeStamp() { return this.timestamp; }

	/**
	 * Sets the entity timestamp.
	 *
	 * @param timestamp
	 */
	public void setTimeStamp(int timestamp) {
		this.timestamp = timestamp;
	}

	/**
     * Gets the signature.
     *
     * @return The signature.
     */
    public Signature getSignature() { return this.signature; }

    /**
     * Sets the signature.
     *
     * @param signature The signature.
     */
    public void setSignature(final Signature signature) { this.signature = signature; }

    //endregion

    @Override
    public void serialize(final Serializer serializer) {
        if (null == this.signature)
            throw new SerializationException("cannot serialize a transaction without a signature");

        this.serialize(serializer, true);
    }

    /**
     * Serializes this object.
     *
     * @param serializer The serializer to use.
     * @param includeSignature true if the serialization should include the signature.
     */
    private void serialize(final Serializer serializer, boolean includeSignature) {
        serializer.writeInt("type", this.getType());
        serializer.writeInt("version", this.getVersion());
        serializer.writeInt("timestamp", this.getTimeStamp());
        SerializationUtils.writeAccount(serializer, "signer", this.getSigner(), AccountEncoding.PUBLIC_KEY);

        if (includeSignature)
            SerializationUtils.writeSignature(serializer, "signature", this.getSignature());

        this.serializeImpl(serializer);
    }

    /**
     * Serializes derived-class state.
     *
     * @param serializer The serializer to use.
     */
    protected abstract void serializeImpl(final Serializer serializer);

    /**
     * Signs this entity with the owner's private key.
     */
    public void sign() {
        if (!this.signer.getKeyPair().hasPrivateKey())
            throw new InvalidParameterException("cannot sign because signer does not have private key");

        // (1) serialize the entire transaction to a buffer
        byte[] transactionBytes = this.getBytes();

        // (2) sign the buffer
        Signer signer = new Signer(this.signer.getKeyPair());
        this.signature = signer.sign(transactionBytes);
    }

    /**
     * Verifies that this transaction has been signed by the owner's public key.
     */
    public boolean verify() {
        if (null == this.signature)
            throw new CryptoException("cannot verify because signature does not exist");

        Signer signer = new Signer(this.signer.getKeyPair());
        return signer.verify(this.getBytes(), this.signature);
    }

    private byte[] getBytes() {
        return BinarySerializer.serializeToBytes(this.asNonVerifiable());
    }

    /**
     * Returns a non-verifiable serializer for the current entity.
     *
     * @return A non-verifiable serializer.
     */
    public SerializableEntity asNonVerifiable() {
        return new NonVerifiableSerializationAdapter(this);
    }

    /**
     * A serialization adapter for VerifiableEntity that serializes the entity
     * without a signature.
     */
    public static class NonVerifiableSerializationAdapter implements SerializableEntity {

        final VerifiableEntity entity;

        /**
         * Creates a non-verifiable serialization adapter for entity.
         *
         * @param entity The entity.
         */
        public NonVerifiableSerializationAdapter(final VerifiableEntity entity) {
            this.entity = entity;
        }

        @Override
        public void serialize(Serializer serializer) {
            entity.serialize(serializer, false);
        }
    }
}