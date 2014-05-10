package org.nem.core.model;

import org.nem.core.messages.*;
import org.nem.core.serialization.*;
import org.nem.core.time.TimeInstant;

/**
 * A transaction that represents the exchange of funds and/or a message
 * between a sender and a recipient.
 */
public class TransferTransaction extends Transaction {
	private static final int MAX_MESSAGE_SIZE = 1000;

	private static final TransactionValidator DEFAULT_TRANSFER_VERIFIER = new TransactionValidator() {
		@Override
		public boolean validateTransfer(Account signer, Account recipient, Amount amount) {
			return signer.getBalance().compareTo(amount) >= 0;
		}
	};

	private Amount amount;
	private Message message;
	private Account recipient;

	/**
	 * Creates a transfer transaction.
	 *
	 * @param timestamp The transaction timestamp.
	 * @param sender    The transaction sender.
	 * @param recipient The transaction recipient.
	 * @param amount    The transaction amount.
	 * @param message   The transaction message.
	 */
	public TransferTransaction(final TimeInstant timestamp, final Account sender, final Account recipient, final Amount amount, final Message message) {
		super(TransactionTypes.TRANSFER, 1, timestamp, sender);
		this.recipient = recipient;
		this.amount = amount;
		this.message = message;

		if (null == this.recipient)
			throw new IllegalArgumentException("recipient is required");
	}

	/**
	 * Deserializes a transfer transaction.
	 *
	 * @param deserializer The deserializer.
	 */
	public TransferTransaction(final DeserializationOptions options, final Deserializer deserializer) {
		super(TransactionTypes.TRANSFER, options, deserializer);
		this.recipient = Account.readFrom(deserializer, "recipient");
		this.amount = Amount.readFrom(deserializer, "amount");
		this.message = deserializer.readObject("message", MessageFactory.createDeserializer(this.getSigner(), this.getRecipient()));
	}

	/**
	 * Gets the recipient.
	 *
	 * @return The recipient.
	 */
	public Account getRecipient() {
		return this.recipient;
	}

	/**
	 * Gets the transaction amount.
	 *
	 * @return The transaction amount.
	 */
	public Amount getAmount() {
		return this.amount;
	}

	/**
	 * Gets the transaction message.
	 *
	 * @return The transaction message.
	 */
	public Message getMessage() {
		return this.message;
	}

	private int getMessageLength() {
		return null == this.message ? 0 : this.message.getEncodedPayload().length;
	}

	@Override
	public boolean isValid()
	{
		return this.isValid(DEFAULT_TRANSFER_VERIFIER);
	}

	@Override
	public boolean isValid(final TransactionValidator transactionValidator) {
		return super.isValid()
				&& transactionValidator.validateTransfer(this.getSigner(), this.getRecipient(), this.amount.add(this.getFee()))
				&& this.getMessageLength() <= MAX_MESSAGE_SIZE;
	}

	@Override
	protected Amount getMinimumFee() {
		if (GenesisBlock.ACCOUNT.equals(this.getSigner()))
			return Amount.ZERO;

		// TODO: add scaling to Amount
		long amountFee = (long)Math.ceil(this.amount.getNumMicroNem() * 0.001);
		long messageFee = (long)Math.ceil(this.getMessageLength() * 0.005);
		return new Amount(Math.max(1, amountFee + messageFee));
	}

	@Override
	protected void serializeImpl(final Serializer serializer) {
		super.serializeImpl(serializer);
		Account.writeTo(serializer, "recipient", this.recipient);
		Amount.writeTo(serializer, "amount", this.amount);
		serializer.writeObject("message", this.message);
	}

	@Override
	protected void executeCommit() {
		if (0 != this.getMessageLength())
			this.recipient.addMessage(this.message);
	}

	@Override
	protected void undoCommit() {
		if (0 != this.getMessageLength())
			this.recipient.removeMessage(this.message);
	}

	@Override
	protected void transfer(final TransferObserver observer) {
		observer.notifyTransfer(this.getSigner(), this.recipient, this.amount);
		observer.notifyDebit(this.getSigner(), this.getFee());
	}
}