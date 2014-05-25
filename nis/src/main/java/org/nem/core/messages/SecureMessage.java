package org.nem.core.messages;

import org.nem.core.crypto.*;
import org.nem.core.model.*;
import org.nem.core.serialization.*;

import java.util.Arrays;

/**
 * A secure, encrypted message.
 */
public class SecureMessage extends Message {

	private final Account sender;
	private final Account recipient;
	private final byte[] payload;

	private SecureMessage(final Account sender, final Account recipient, final byte[] payload) {
		super(MessageTypes.SECURE);
		this.sender = sender;
		this.recipient = recipient;
		this.payload = payload;
	}

	/**
	 * Creates a new secure message around a decoded payload that should be encrypted.
	 *
	 * @param sender    The message sender.
	 * @param recipient The message recipient.
	 * @param payload   The unencrypted payload.
	 */
	public static SecureMessage fromDecodedPayload(final Account sender, final Account recipient, final byte[] payload) {

		if (!sender.getKeyPair().hasPrivateKey())
			throw new IllegalArgumentException("sender private key is required for creating secure message");

		final Cipher cipher = new Cipher(sender.getKeyPair(), recipient.getKeyPair());
		return new SecureMessage(sender, recipient, cipher.encrypt(payload));
	}

	/**
	 * Creates a new secure message around an encoded payload that is already encrypted.
	 *
	 * @param sender    The message sender.
	 * @param recipient The message recipient.
	 * @param payload   The encrypted payload.
	 */
	public static SecureMessage fromEncodedPayload(final Account sender, final Account recipient, final byte[] payload) {
		return new SecureMessage(sender, recipient, payload);
	}

	/**
	 * Deserializes a secure message.
	 *
	 * @param deserializer The deserializer.
	 */
	public SecureMessage(final Deserializer deserializer) {
		super(MessageTypes.SECURE);
		this.sender = null;
		this.recipient = null;
		this.payload = deserializer.readBytes("payload");
	}

	@Override
	public boolean canDecode() {
		return this.recipient.getKeyPair().hasPrivateKey();
	}

	@Override
	public byte[] getEncodedPayload() {
		return this.payload;
	}

	@Override
	public byte[] getDecodedPayload() {
		if (!this.recipient.getKeyPair().hasPrivateKey())
			return null;

		final Cipher cipher = new Cipher(this.sender.getKeyPair(), this.recipient.getKeyPair());
		return cipher.decrypt(this.payload);
	}

	@Override
	public void serialize(final Serializer serializer) {
		super.serialize(serializer);
		serializer.writeBytes("payload", this.payload);
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(this.payload);
	}

	@Override
	public boolean equals(final Object obj) {
		if (!(obj instanceof SecureMessage))
			return false;

		final SecureMessage rhs = (SecureMessage)obj;
		return Arrays.equals(this.payload, rhs.payload)
				&& this.sender.getAddress().equals(rhs.sender.getAddress())
				&& this.recipient.getAddress().equals(rhs.recipient.getAddress());
	}

	private static class SecureMessagePayload implements SerializableEntity {



		@Override
		public void serialize(Serializer serializer) {

		}
	}
}

