package org.nem.nis.test;

import org.nem.core.model.*;
import org.nem.core.model.primitive.Amount;
import org.nem.core.test.Utils;
import org.nem.core.time.TimeInstant;

import java.util.Arrays;

/**
 * Factory class used to create random (concrete) transactions.
 */
public class RandomTransactionFactory {

	/**
	 * Creates a transfer transaction.
	 *
	 * @return The transfer.
	 */
	public static TransferTransaction createTransfer() {
		return new TransferTransaction(
				TimeInstant.ZERO,
				Utils.generateRandomAccount(),
				Utils.generateRandomAccount(),
				Amount.fromNem(111),
				null);
	}

	/**
	 * Creates an importance transfer transaction.
	 *
	 * @return The importance transfer.
	 */
	public static ImportanceTransferTransaction createImportanceTransfer() {
		return new ImportanceTransferTransaction(
				TimeInstant.ZERO,
				Utils.generateRandomAccount(),
				ImportanceTransferTransaction.Mode.Activate,
				Utils.generateRandomAccount());
	}

	/**
	 * Creates a multisig aggregate modification.
	 *
	 * @return The multisig aggregate modification.
	 */
	public static MultisigAggregateModificationTransaction createMultisigModification() {
		return new MultisigAggregateModificationTransaction(
				TimeInstant.ZERO,
				Utils.generateRandomAccount(),
				Arrays.asList(new MultisigModification(MultisigModificationType.Add, Utils.generateRandomAccount())));
	}
}