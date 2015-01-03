package org.nem.nis.test;

import org.mockito.Mockito;
import org.nem.core.crypto.Hash;
import org.nem.core.model.*;
import org.nem.core.model.primitive.Amount;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.test.Utils;
import org.nem.core.time.TimeInstant;
import org.nem.nis.cache.AccountStateCache;
import org.nem.nis.state.AccountState;
import org.nem.nis.validators.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MultisigTestContext {
	private final AccountStateCache accountCache = Mockito.mock(AccountStateCache.class);
	private final MultisigSignerModificationTransactionValidator multisigSignerModificationTransactionValidator = new MultisigSignerModificationTransactionValidator(this.accountCache);
	private final MultisigTransactionSignerValidator multisigTransactionSignerValidator = new MultisigTransactionSignerValidator(this.accountCache);
	private final MultisigNonOperationalValidator validator = new MultisigNonOperationalValidator(this.accountCache);
	private final MultisigSignaturesPresentValidator multisigSignaturesPresentValidator;
	private final MultisigSignatureValidator multisigSignatureValidator;

	private final List<Transaction> transactionList = new ArrayList<>();

	public final Account signer = Utils.generateRandomAccount();
	public final Account multisig = Utils.generateRandomAccount();
	private final Account recipient = Utils.generateRandomAccount();
	public final Account dummy = Utils.generateRandomAccount();

	public MultisigTestContext() {
		this.multisigSignaturesPresentValidator = new MultisigSignaturesPresentValidator(this.accountCache);
		this.multisigSignatureValidator = new MultisigSignatureValidator(this.accountCache, () -> this.transactionList);
		this.addState(this.signer);
		this.addState(this.multisig);
		this.addState(this.dummy);
	}

	public MultisigSignerModificationTransaction createMultisigSignerModificationTransaction(final MultisigModificationType modificationType) {
		final MultisigSignerModificationTransaction transaction = new MultisigSignerModificationTransaction(
				TimeInstant.ZERO,
				this.multisig,
				Arrays.asList(new MultisigModification(modificationType, this.signer))
				);
		transaction.sign();

		return transaction;
	}

	public MultisigTransaction createMultisigTransferTransaction() {
		final TransferTransaction otherTransaction = new TransferTransaction(TimeInstant.ZERO, this.multisig, this.recipient, Amount.fromNem(123), null);
		final MultisigTransaction transaction = new MultisigTransaction(TimeInstant.ZERO, this.signer, otherTransaction);
		transaction.sign();

		this.transactionList.add(transaction);

		return transaction;
	}

	public void addSignature(final Account signatureSigner, final MultisigTransaction multisigTransaction) {
		multisigTransaction.addSignature(new MultisigSignatureTransaction(TimeInstant.ZERO,
				signatureSigner,
				HashUtils.calculateHash(multisigTransaction.getOtherTransaction())));
	}

	public Transaction createMultisigSignature(final Hash otherTransactionHash, final Account signatureIssuer) {
		return new MultisigSignatureTransaction(
				TimeInstant.ZERO,
				signatureIssuer,
				otherTransactionHash
		);
	}

	public AccountState addState(final Account account) {
		final Address address = account.getAddress();
		final AccountState state = new AccountState(address);
		Mockito.when(this.accountCache.findStateByAddress(address)).thenReturn(state);
		return state;
	}

	public void makeCosignatory(final Account signer, final Account multisig, final BlockHeight blockHeight) {
		this.accountCache.findStateByAddress(signer.getAddress()).getMultisigLinks().addMultisig(multisig.getAddress(), blockHeight);
		this.accountCache.findStateByAddress(multisig.getAddress()).getMultisigLinks().addCosignatory(signer.getAddress(), blockHeight);
	}

	public boolean debitPredicate(final Account account, final Amount amount) {
		final Amount balance = this.accountCache.findStateByAddress(account.getAddress()).getAccountInfo().getBalance();
		return balance.compareTo(amount) >= 0;
	}

	// forward to validators
	public ValidationResult validateSignaturePresent(final Transaction transaction, final BlockHeight blockHeight) {
		return this.multisigSignaturesPresentValidator.validate(transaction, new ValidationContext(blockHeight, this::debitPredicate));
	}

	public ValidationResult validateNonOperational(final Transaction transaction) {
		return validator.validate(transaction, new ValidationContext((final Account account, final Amount amount) -> true));
	}

	public ValidationResult validateMultisigSignature(final Transaction transaction, final BlockHeight height) {
		return this.multisigSignatureValidator.validate(transaction, new ValidationContext(height, this::debitPredicate));
	}

	public ValidationResult validateSignerModification(final Transaction transaction) {
		return multisigSignerModificationTransactionValidator.validate(transaction, new ValidationContext((final Account account, final Amount amount) -> true));
	}

	public ValidationResult validateTransaction(final Transaction transaction, final BlockHeight blockHeight) {
		return multisigTransactionSignerValidator.validate(transaction, new ValidationContext(blockHeight, (final Account account, final Amount amount) -> true));
	}
}
