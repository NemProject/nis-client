package org.nem.nis.poi.graph;


/**
 * A representation of a NXT transaction.
 */
public class NxtTransaction {
	private final long height;
	private final long senderId;
	private final long recipientId;
	private final long amount;

	/**
	 * Creates a new NXT transaction.
	 *
	 * @param height The transaction height.
	 * @param senderId The transaction sender.
	 * @param recipientId The transaction recipient.
	 * @param amount The transaction amount.
	 */
	public NxtTransaction(final long height, final long senderId, final long recipientId, final long amount) {
		this.height = height;
		this.senderId = senderId;
		this.recipientId = recipientId;
		this.amount = amount;
	}

	/**
	 * Gets the height.
	 *
	 * @return The height.
	 */
	public long getHeight() {
		return this.height;
	}

	/**
	 * Gets the sender id.
	 *
	 * @return The sender id.
	 */
	public long getSenderId() {
		return this.senderId;
	}

	/**
	 * Gets the recipient id.
	 *
	 * @return The recipient id.
	 */
	public long getRecipientId() {
		return this.recipientId;
	}

	/**
	 * Gets the amount.
	 *
	 * @return The amount.
	 */
	public long getAmount() {
		return this.amount;
	}
}
