package org.nem.nis.validators;

import org.nem.core.model.*;
import org.nem.nis.cache.ReadOnlyAccountStateCache;
import org.nem.nis.state.ReadOnlyAccountState;

import java.util.*;
import java.util.stream.Collectors;

/**
 * A transaction validator that validates that a multisig transaction has all required signatures.
 * - A non-delete aggregate modification (or any other supported transaction) is signed by all cosigners.
 * - A delete aggregate modification is signed by all cosigners except the cosigner being deleted (when at least one cosigner remains).
 * - A delete aggregate modification of the last cosigner is signed by that cosigner.
 * - A delete aggregate modification can delete at most one account.
 * <br>
 * This validator should only be used during block creation, or when receiving a block.
 */
public class MultisigSignaturesPresentValidator implements SingleTransactionValidator {
	private final ReadOnlyAccountStateCache stateCache;

	/**
	 * Creates a new validator.
	 *
	 * @param stateCache The account state cache.
	 */
	public MultisigSignaturesPresentValidator(final ReadOnlyAccountStateCache stateCache) {
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
		final ReadOnlyAccountState multisigAddress = this.stateCache.findStateByAddress(transaction.getOtherTransaction().getSigner().getAddress());

		final HashSet<Address> signerAddresses = new HashSet<>();
		signerAddresses.add(transaction.getSigner().getAddress());

		// note that we don't need to revalidate signatures here because a multisig signature transaction
		// does not allow the addition of invalid signatures
		signerAddresses.addAll(transaction.getCosignerSignatures().stream().map(s -> s.getSigner().getAddress()).collect(Collectors.toList()));

		final List<Address> accountsForRemoval = getRemovedAddresses(transaction);
		if (accountsForRemoval.size() > 1) {
			return ValidationResult.FAILURE_MULTISIG_MODIFICATION_MULTIPLE_DELETES;
		}

		final Address accountForRemoval = accountsForRemoval.isEmpty() ? null : accountsForRemoval.get(0);
		final Set<Address> expectedSignerAddresses = new HashSet<>(multisigAddress.getMultisigLinks().getCosignatories());
		if (null != accountForRemoval && expectedSignerAddresses.size() > 1) {
			signerAddresses.remove(accountForRemoval);
			expectedSignerAddresses.remove(accountForRemoval);
		}

		return signerAddresses.equals(expectedSignerAddresses)
				? ValidationResult.SUCCESS
				: ValidationResult.FAILURE_MULTISIG_INVALID_COSIGNERS;
	}

	private static List<Address> getRemovedAddresses(final MultisigTransaction transaction) {
		if (TransactionTypes.MULTISIG_AGGREGATE_MODIFICATION != transaction.getOtherTransaction().getType()) {
			return Collections.emptyList();
		}

		final MultisigAggregateModificationTransaction modificationTransaction = (MultisigAggregateModificationTransaction)transaction.getOtherTransaction();
		return modificationTransaction.getModifications().stream()
				.filter(m -> m.getModificationType() == MultisigModificationType.Del)
				.map(m -> m.getCosignatory().getAddress())
				.collect(Collectors.toList());
	}
}