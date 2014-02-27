package org.nem.core.transactions;

import org.nem.core.model.*;
import org.nem.core.serialization.*;

import java.security.InvalidParameterException;

/**
 * A transaction that represents the exchange of funds and/or a message
 * between a sender and a recipient.
 */
public class TransferTransaction extends Transaction {
    private static final int MAX_MESSAGE_SIZE = 1000;

    private long amount;
    private byte[] message;
    private Account recipient;

    /**
     * Creates a transfer transaction.
     *
     * @param sender The transaction sender.
     * @param recipient The transaction recipient.
     * @param amount The transaction amount.
     * @param message The transaction message.
     */
    public TransferTransaction(final Account sender, final Account recipient, final long amount, final byte[] message) {
        super(TransactionTypes.TRANSFER, 1, sender);
        this.recipient = recipient;
        this.amount = amount;
        this.message = null == message ? new byte[] { } : message;

        if (null == this.recipient)
            throw new InvalidParameterException("recipient is required");
    }

    /**
     * Deserializes a transfer transaction.
     *
     * @param deserializer The deserializer.
     */
    public TransferTransaction(final Deserializer deserializer) {
        super(TransactionTypes.TRANSFER, deserializer);
        this.recipient = SerializationUtils.readAccount(deserializer, "recipient");
        this.amount = deserializer.readLong("amount");
        this.message = deserializer.readBytes("message");
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
    public long getAmount() { return this.amount; }

    /**
     * Gets the transaction message.
     *
     * @return The transaction message.
     */
    public byte[] getMessage() { return this.message; }

    @Override
    public boolean isValid() {
        if (this.getSigner().getBalance() < this.amount + this.getFee())
            return false;

        return message.length <= MAX_MESSAGE_SIZE;
    }

    @Override
    protected long getMinimumFee() {
        long amountFee = (long)Math.ceil(this.amount * 0.001);
        long messageFee = (long)Math.ceil(this.message.length * 0.005);
        return amountFee + messageFee;
    }

    @Override
    protected void serializeImpl(final Serializer serializer) {
        super.serializeImpl(serializer);
        SerializationUtils.writeAccount(serializer, "recipient", this.recipient);
        serializer.writeLong("amount", this.amount);
        serializer.writeBytes("message", this.message);
    }

    @Override
    public void execute() {
        this.getSigner().incrementBalance(-this.amount - this.getFee());
        this.recipient.incrementBalance(this.amount);

        if (0 != this.message.length)
            this.recipient.addMessage(this.getSigner(), this.message);
    }
}