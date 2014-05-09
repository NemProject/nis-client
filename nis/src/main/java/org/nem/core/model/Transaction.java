package org.nem.core.model;

import org.nem.core.serialization.*;
import org.nem.core.time.TimeInstant;

import java.util.*;

/**
 * An abstract transaction class that serves as the base class of all NEM transactions.
 */
public abstract class Transaction extends VerifiableEntity implements Comparable<Transaction> {

	private Amount fee = Amount.ZERO;
	private TimeInstant deadline = TimeInstant.ZERO;
	private final List<TransferObserver> transferObservers = new ArrayList<>();
	private final TransferObserver transferObserver = new TransferObserverAggregate();

	/**
	 * Creates a new transaction.
	 *
	 * @param type      The transaction type.
	 * @param version   The transaction version.
	 * @param timestamp The transaction timestamp.
	 * @param sender    The transaction sender.
	 */
	public Transaction(final int type, final int version, final TimeInstant timestamp, final Account sender) {
		super(type, version, timestamp, sender);
	}

	/**
	 * Deserializes a new transaction.
	 *
	 * @param type         The transaction type.
	 * @param deserializer The deserializer to use.
	 */
	public Transaction(final int type, final DeserializationOptions options, final Deserializer deserializer) {
		super(type, options, deserializer);
		this.fee = Amount.readFrom(deserializer, "fee");
		this.deadline = TimeInstant.readFrom(deserializer, "deadline");
	}

	//region Setters and Getters

	/**
	 * Gets the fee.
	 *
	 * @return The fee.
	 */
	public Amount getFee() {
		return this.fee.compareTo(this.getMinimumFee()) < 0
				? this.getMinimumFee()
				: this.fee;
	}

	/**
	 * Sets the fee.
	 *
	 * @param fee The desired fee.
	 */
	public void setFee(final Amount fee) {
		this.fee = fee;
	}

	/**
	 * Gets the deadline.
	 *
	 * @return The deadline.
	 */
	public TimeInstant getDeadline() {
		return this.deadline;
	}

	/**
	 * Sets the deadline.
	 *
	 * @param deadline The desired deadline.
	 */
	public void setDeadline(final TimeInstant deadline) {
		this.deadline = deadline;
	}

	//endregion

	@Override
	public int compareTo(Transaction rhs) {
		int[] comparisonResults = new int[] {
				Integer.compare(this.getType(), rhs.getType()),
				Integer.compare(this.getVersion(), rhs.getVersion()),
				this.getTimeStamp().compareTo(rhs.getTimeStamp()),
				this.getFee().compareTo(rhs.getFee())
		};

		for (int result : comparisonResults) {
			if (result != 0)
				return result;
		}

		return 0;
	}

	@Override
	protected void serializeImpl(final Serializer serializer) {
		Amount.writeTo(serializer, "fee", this.getFee());
		TimeInstant.writeTo(serializer, "deadline", this.getDeadline());
	}

	/**
	 * Executes the transaction.
	 *
	 * @param commit true if changes should be committed by the default action.
	 */
	public final void execute(boolean commit) {
		this.executeTransfer(this.transferObserver);
		if (!commit)
			return;

		this.executeTransfer(new CommitTransferObserver());
		this.executeCommit();
	}

	/**
	 * Executes all transfers using the specified observer.
	 *
	 * @param observer The transfer observer.
	 */
	protected abstract void executeTransfer(final TransferObserver observer);

	/**
	 * Performs any other actions required to commit the transaction.
	 */
	protected abstract void executeCommit();

	/**
	 * Performs any other actions required to undo the transaction
	 *
	 * @param commit true if changes should be committed by the default action.
	 */
	public final void undo(boolean commit) {
		this.undoTransfer(this.transferObserver);
		if (!commit)
			return;

		this.undoTransfer(new CommitTransferObserver());
		this.undoCommit();
	}

	/**
	 * Undoes all transfers using the specified observer.
	 */
	protected abstract void undoTransfer(final TransferObserver observer);

	/**
	 * Performs any other actions required to undo the transaction.
	 */
	protected abstract void undoCommit();

	/**
	 * Determines if this transaction is valid.
	 *
	 * @return true if this transaction is valid.
	 */
	public boolean isValid() {
		return this.deadline.compareTo(this.getTimeStamp()) > 0
				&& this.deadline.compareTo(this.getTimeStamp().addDays(1)) < 1;
	}

	/**
	 * Gets the minimum fee for this transaction.
	 *
	 * @return The minimum fee.
	 */
	protected abstract Amount getMinimumFee();

	/**
	 * Subscribes the observer to transfers initiated by this transaction.
	 *
	 * @param observer The observer.
	 */
	public void subscribe(final TransferObserver observer) {
		this.transferObservers.add(observer);
	}

	/**
	 * Unsubscribes the observer from transfers initiated by this transaction.
	 *
	 * @param observer The observer.
	 */
	public void unsubscribe(final TransferObserver observer) {
		this.transferObservers.remove(observer);
	}

	private static class CommitTransferObserver implements TransferObserver {

		@Override
		public void notifyTransfer(final Account sender, final Account recipient, final Amount amount) {
			this.notifyDebit(sender, amount);
			this.notifyCredit(recipient, amount);
		}

		@Override
		public void notifyCredit(final Account account, final Amount amount) {
			account.incrementBalance(amount);
		}

		@Override
		public void notifyDebit(final Account account, final Amount amount) {
			account.decrementBalance(amount);
		}
	}

	private class TransferObserverAggregate implements TransferObserver {

		@Override
		public void notifyTransfer(final Account sender, final Account recipient, final Amount amount) {
			transferObservers.stream().forEach(o -> o.notifyTransfer(sender, recipient, amount));
		}

		@Override
		public void notifyCredit(final Account account, final Amount amount) {
			transferObservers.stream().forEach(o -> o.notifyCredit(account, amount));
		}

		@Override
		public void notifyDebit(final Account account, final Amount amount) {
			transferObservers.stream().forEach(o -> o.notifyDebit(account, amount));
		}
	}
}