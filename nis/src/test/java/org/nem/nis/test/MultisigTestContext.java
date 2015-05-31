package org.nem.nis.test;

import org.mockito.Mockito;
import org.nem.core.model.*;
import org.nem.core.model.primitive.Amount;
import org.nem.core.test.Utils;
import org.nem.core.time.TimeInstant;
import org.nem.nis.cache.*;
import org.nem.nis.state.*;
import org.nem.nis.validators.ValidationContext;
import org.nem.nis.validators.transaction.*;

import java.util.*;

public class MultisigTestContext {
	private final AccountStateCache accountStateCache = Mockito.mock(AccountStateCache.class);
	private final AccountCache accountCache = Mockito.mock(AccountCache.class);
	private final MultisigAggregateModificationTransactionValidator multisigAggregateModificationTransactionValidator =
			new MultisigAggregateModificationTransactionValidator(this.accountStateCache);
	private final MultisigTransactionSignerValidator multisigTransactionSignerValidator = new MultisigTransactionSignerValidator(this.accountStateCache);
	private final MultisigNonOperationalValidator validator = new MultisigNonOperationalValidator(this.accountStateCache);
	private final MultisigSignaturesPresentValidator multisigSignaturesPresentValidator;

	@SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
	private final List<Transaction> transactionList = new ArrayList<>();

	public final Account signer = Utils.generateRandomAccount();
	public final Account multisig = Utils.generateRandomAccount();
	private final Account recipient = Utils.generateRandomAccount();
	public final Account dummy = Utils.generateRandomAccount();

	public MultisigTestContext() {
		this.multisigSignaturesPresentValidator = new MultisigSignaturesPresentValidator(this.accountStateCache);
		this.addState(this.signer);
		this.addState(this.multisig);
		this.addState(this.dummy);
	}

	public MultisigTransaction createMultisigModificationTransaction(final List<MultisigCosignatoryModification> modifications) {
		final MultisigAggregateModificationTransaction otherTransaction = new MultisigAggregateModificationTransaction(
				TimeInstant.ZERO,
				this.multisig,
				modifications);

		final MultisigTransaction transaction = new MultisigTransaction(TimeInstant.ZERO, this.signer, otherTransaction);
		transaction.sign();

		this.transactionList.add(transaction);
		return transaction;
	}

	public void modifyMultisigAccount(final int minCosignatories, final int numCosignatories) {
		if (!(minCosignatories == 0 && numCosignatories == 0) &&
			!(0 < minCosignatories && minCosignatories <= numCosignatories)) {
			throw new IllegalArgumentException(String.format("minimum number of cosignatories is out of range: %d", minCosignatories));
		}

		final MultisigLinks multisigLinks = this.getMultisigLinks(this.multisig);
		if (minCosignatories == 0) {
			multisigLinks.getCosignatories().stream().forEach(address -> this.removeCosignatory(this.accountCache.findByAddress(address), this.multisig));
			return;
		}

		this.makeCosignatory(this.signer, this.multisig);
		for (int i = 1; i < numCosignatories; i++) {
			final Account account = this.addAccount();
			this.makeCosignatory(account, this.multisig);
		}

		this.adjustMinCosignatories(minCosignatories - numCosignatories);
	}

	public MultisigAggregateModificationTransaction createTypedMultisigModificationTransaction(final List<MultisigCosignatoryModification> modifications) {
		return createTypedMultisigModificationTransaction(modifications, null);
	}

	public MultisigAggregateModificationTransaction createTypedMultisigModificationTransaction(
			final List<MultisigCosignatoryModification> modifications,
			final MultisigMinCosignatoriesModification minCosignatoriesModification) {
		return new MultisigAggregateModificationTransaction(
				TimeInstant.ZERO,
				this.multisig,
				modifications,
				minCosignatoriesModification);
	}

	public MultisigTransaction createMultisigTransferTransaction() {
		return this.createMultisigTransferTransaction(this.signer);
	}

	public MultisigTransaction createMultisigTransferTransaction(final Account multisigSigner) {
		final TransferTransaction otherTransaction = new TransferTransaction(TimeInstant.ZERO, this.multisig, this.recipient, Amount.fromNem(123), null);
		final MultisigTransaction transaction = new MultisigTransaction(TimeInstant.ZERO, multisigSigner, otherTransaction);
		transaction.sign();

		this.transactionList.add(transaction);

		return transaction;
	}

	public void addSignature(final Account signatureSigner, final MultisigTransaction multisigTransaction) {
		multisigTransaction.addSignature(new MultisigSignatureTransaction(
				TimeInstant.ZERO,
				signatureSigner,
				this.multisig,
				multisigTransaction.getOtherTransaction()));
	}

	public void addSignatures(final MultisigTransaction multisigTransaction, final int numSignatures) {
		this.getCosignatories(this.multisig).stream()
				.filter(address -> !address.equals(multisigTransaction.getSigner().getAddress()))
				.limit(numSignatures)
				.forEach(address -> this.addSignature(this.accountCache.findByAddress(address), multisigTransaction));
	}

	public AccountState addState(final Account account) {
		final Address address = account.getAddress();
		final AccountState state = new AccountState(address);
		Mockito.when(this.accountStateCache.findStateByAddress(address)).thenReturn(state);
		return state;
	}

	public Account addAccount() {
		final Account account = Utils.generateRandomAccount();
		Mockito.when(this.accountCache.findByAddress(account.getAddress())).thenReturn(account);
		this.addState(account);
		return account;
	}

	public void adjustMinCosignatories(final int relativeChange) {
		this.getMultisigLinks(this.multisig).incrementMinCosignatoriesBy(relativeChange);
	}

	public MultisigLinks getMultisigLinks(final Account multisig) {
		return this.accountStateCache.findStateByAddress(multisig.getAddress()).getMultisigLinks();
	}

	public Collection<Address> getCosignatories(final Account multisig) {
		return this.accountStateCache.findStateByAddress(multisig.getAddress()).getMultisigLinks().getCosignatories();
	}

	public void makeCosignatory(final Account cosignatory, final Account multisig) {
		this.accountStateCache.findStateByAddress(cosignatory.getAddress()).getMultisigLinks().addCosignatoryOf(multisig.getAddress());
		this.accountStateCache.findStateByAddress(multisig.getAddress()).getMultisigLinks().addCosignatory(cosignatory.getAddress());
	}

	public void removeCosignatory(final Account cosignatory, final Account multisig) {
		this.accountStateCache.findStateByAddress(cosignatory.getAddress()).getMultisigLinks().removeCosignatoryOf(multisig.getAddress());
		this.accountStateCache.findStateByAddress(multisig.getAddress()).getMultisigLinks().removeCosignatory(cosignatory.getAddress());
	}

	public boolean debitPredicate(final Account account, final Amount amount) {
		final Amount balance = this.accountStateCache.findStateByAddress(account.getAddress()).getAccountInfo().getBalance();
		return balance.compareTo(amount) >= 0;
	}

	// forward to validators
	public ValidationResult validateSignaturePresent(final MultisigTransaction transaction) {
		return this.multisigSignaturesPresentValidator.validate(transaction, new ValidationContext(this::debitPredicate));
	}

	public ValidationResult validateNonOperational(final Transaction transaction) {
		return this.validator.validate(transaction, new ValidationContext(DebitPredicates.Throw));
	}

	public ValidationResult validateMultisigModification(final MultisigAggregateModificationTransaction transaction) {
		return this.multisigAggregateModificationTransactionValidator.validate(transaction, new ValidationContext(DebitPredicates.Throw));
	}

	public ValidationResult validateTransaction(final MultisigTransaction transaction) {
		return this.multisigTransactionSignerValidator.validate(transaction, new ValidationContext(DebitPredicates.Throw));
	}
}
