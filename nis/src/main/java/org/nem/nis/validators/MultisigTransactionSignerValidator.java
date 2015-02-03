package org.nem.nis.validators;

import org.nem.core.model.*;
import org.nem.nis.cache.ReadOnlyAccountStateCache;
import org.nem.nis.state.ReadOnlyAccountState;

/**
 * A transaction validator that validates that:
 * - A multisig signature transaction is signed by a cosigner of the multisig account.
 */
public class MultisigTransactionSignerValidator implements SingleTransactionValidator {
	private final ReadOnlyAccountStateCache stateCache;

	/**
	 * Creates a validator.
	 *
	 * @param stateCache The account state cache.
	 */
	public MultisigTransactionSignerValidator(final ReadOnlyAccountStateCache stateCache) {
		this.stateCache = stateCache;
	}

	@Override
	public ValidationResult validate(final Transaction transaction, final ValidationContext context) {
		if (TransactionTypes.MULTISIG != transaction.getType()) {
			return ValidationResult.SUCCESS;
		}

		return this.validate((MultisigTransaction)transaction);
	}

	private ValidationResult validate(final MultisigTransaction transaction) {
		final ReadOnlyAccountState cosignerState = this.stateCache.findStateByAddress(transaction.getSigner().getAddress());

		if (!cosignerState.getMultisigLinks().isCosignatoryOf(transaction.getOtherTransaction().getSigner().getAddress())) {
			return ValidationResult.FAILURE_MULTISIG_NOT_A_COSIGNER;
		}

		return ValidationResult.SUCCESS;
	}
}