package org.nem.nis.validators;

import org.nem.core.crypto.Hash;
import org.nem.core.model.*;
import org.nem.nis.BlockMarkerConstants;
import org.nem.nis.cache.ReadOnlyAccountStateCache;
import org.nem.nis.state.ReadOnlyAccountState;

public class MultisigSignaturesPresentValidator implements SingleTransactionValidator {
	private final ReadOnlyAccountStateCache stateCache;
	private final boolean blockCreation;

	public MultisigSignaturesPresentValidator(final ReadOnlyAccountStateCache stateCache, final boolean blockCreation) {
		this.stateCache = stateCache;
		this.blockCreation = blockCreation;
	}

	@Override
	public ValidationResult validate(final Transaction transaction, final ValidationContext context) {
		if (TransactionTypes.MULTISIG != transaction.getType()) {
			return ValidationResult.SUCCESS;
		}

		if (context.getBlockHeight().getRaw() < BlockMarkerConstants.BETA_MULTISIG_FORK) {
			return ValidationResult.FAILURE_ENTITY_UNUSABLE;
		}

		return this.validate((MultisigTransaction)transaction, context);
	}

	private ValidationResult validate(final MultisigTransaction transaction, final ValidationContext context) {
		final ReadOnlyAccountState senderState = this.stateCache.findStateByAddress(transaction.getSigner().getAddress());

		if (!senderState.getMultisigLinks().isCosignatoryOf(transaction.getOtherTransaction().getSigner().getAddress())) {
			return ValidationResult.FAILURE_MULTISIG_NOT_A_COSIGNER;
		}

		// if this is not block creation, we don't want to check if signature of all cosignatories are present
		// TODO 20141201 J-G: why?
		// TODO 20141202 G-J: cause we might not have proper MultisigSignatures YET, and we want to
		// be able to add MultisigTransaction itself to list of unconfirmed transactions.
		if (!this.blockCreation) {
			return ValidationResult.SUCCESS;
		}

		// TODO 20131204 G-G: if we'll be adding MultisigSignature to MultisigTransaction,
		// whole piece below probably won't be necessary

		final ReadOnlyAccountState multisigAddress = this.stateCache.findStateByAddress(transaction.getOtherTransaction().getSigner().getAddress());
		if (multisigAddress.getMultisigLinks().getCosignatories().size() == 1) {
			return ValidationResult.SUCCESS;
		}

		final Hash transactionHash = transaction.getOtherTransactionHash();
		// this loop could be done using reduce, but I'm leaving it like this for readability
		// TODO: this probably should be done differently, as right now it allows more MultisigSignatures, than there are actual cosigners
		for (final Address cosignerAddress : multisigAddress.getMultisigLinks().getCosignatories()) {
			if (cosignerAddress.equals(transaction.getSigner().getAddress())) {
				continue;
			}
			final boolean hasCosigner = transaction.getCosignerSignatures().stream()
					.anyMatch(
							t -> t.getOtherTransactionHash().equals(transactionHash) &&
									t.getSigner().getAddress().equals(cosignerAddress)
					);

			if (!hasCosigner) {
				return ValidationResult.FAILURE_MULTISIG_MISSING_COSIGNERS;
			}
		}

		return ValidationResult.SUCCESS;
	}
}
