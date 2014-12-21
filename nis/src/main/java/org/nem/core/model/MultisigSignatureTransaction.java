package org.nem.core.model;

import org.nem.core.crypto.Hash;
import org.nem.core.model.observers.TransactionObserver;
import org.nem.core.model.primitive.Amount;
import org.nem.core.serialization.*;
import org.nem.core.time.TimeInstant;

import java.util.*;

/**
 * A multisig signature transaction.
 */
public class MultisigSignatureTransaction extends Transaction implements SerializableEntity {
	private final Hash otherTransactionHash;

	/**
	 * Creates a multisig signature transaction.
	 *
	 * @param timeStamp The transaction timestamp.
	 * @param sender The transaction sender.
	 * @param otherTransactionHash The hash of the other transaction.
	 */
	public MultisigSignatureTransaction(
			final TimeInstant timeStamp,
			final Account sender,
			final Hash otherTransactionHash) {
		super(TransactionTypes.MULTISIG_SIGNATURE, 1, timeStamp, sender);
		this.otherTransactionHash = otherTransactionHash;
	}

	/**
	 * Deserializes a multisig signature transaction.
	 *
	 * @param options The deserialization options.
	 * @param deserializer The deserializer.
	 */
	public MultisigSignatureTransaction(final DeserializationOptions options, final Deserializer deserializer) {
		super(TransactionTypes.MULTISIG_SIGNATURE, options, deserializer);
		this.otherTransactionHash = deserializer.readObject("otherHash", Hash::new);
	}

	/**
	 * Gets the hash of the other transaction.
	 *
	 * @return The hash of the other transaction.
	 */
	public Hash getOtherTransactionHash() {
		return this.otherTransactionHash;
	}

	@Override
	protected void transfer(final TransactionObserver observer) {
	}

	@Override
	protected Amount getMinimumFee() {
		return Amount.ZERO;
	}

	@Override
	protected Collection<Account> getOtherAccounts() {
		// TODO 20141220 J-G: should review / test this
		return new ArrayList<>();
	}

	@Override
	public boolean equals(final Object obj) {
		if (!(obj instanceof MultisigSignatureTransaction)) {
			return false;
		}

		final MultisigSignatureTransaction rhs = (MultisigSignatureTransaction)obj;
		return (0 == rhs.compareTo(rhs));
	}

	@Override
	public int compareTo(final Transaction rhs) {
		// first sort by fees (lowest first) and then timestamps (newest first)
		int result = super.compareTo(rhs);
		if (result != 0) {
			return result;
		}

		return this.getSignature().compareTo(rhs.getSignature());
	}

	@Override
	protected void serializeImpl(final Serializer serializer) {
		super.serializeImpl(serializer);
		serializer.writeObject("otherHash", this.getOtherTransactionHash());
	}
}
