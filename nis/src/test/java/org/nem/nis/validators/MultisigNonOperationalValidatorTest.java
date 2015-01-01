package org.nem.nis.validators;

import org.hamcrest.core.IsEqual;
import org.junit.Assert;
import org.junit.Test;
import org.nem.core.model.*;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.test.MockTransaction;
import org.nem.core.test.Utils;
import org.nem.core.time.TimeInstant;
import org.nem.nis.test.MultisigTestContext;

import java.util.Arrays;

public class MultisigNonOperationalValidatorTest {

	@Test
	public void nonMultisigAccountCanValidateAnyTransaction() {
		// Arrange:
		final MultisigTestContext context = new MultisigTestContext();
		final Account account = Utils.generateRandomAccount();
		final Transaction transaction = new MockTransaction(account);
		context.addState(account);

		// Act:
		final ValidationResult result = context.validateNonOperational(transaction);

		// Assert
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
	}

	@Test
	public void canValidateChildTransaction() {
		// Arrange:
		final MultisigTestContext context = new MultisigTestContext();
		final Account multisig = Utils.generateRandomAccount();
		final Transaction transaction = new MockTransaction(multisig);
		final Account cosignatory = Utils.generateRandomAccount();
		context.addState(multisig);
		context.addState(cosignatory);
		context.makeCosignatory(cosignatory, multisig, BlockHeight.ONE);

		// note, we're not signing transaction which means it's a child transaction

		// Act:
		final ValidationResult result = context.validateNonOperational(transaction);

		// Assert
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
	}

	@Test
	public void multisigAccountCannotMakeMostTransactions() {
		// Arrange:
		final MultisigTestContext context = new MultisigTestContext();
		final Account multisig = Utils.generateRandomAccount();
		final Transaction transaction = new MockTransaction(multisig);
		final Account cosignatory = Utils.generateRandomAccount();
		context.addState(multisig);
		context.addState(cosignatory);
		context.makeCosignatory(cosignatory, multisig, BlockHeight.ONE);

		transaction.sign();

		// Act:
		final ValidationResult result = context.validateNonOperational(transaction);

		// Assert
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_TRANSACTION_NOT_ALLOWED_FOR_MULTISIG));
	}

	@Test
	public void multisigAccountCanIssueMultisigModification() {
		// Arrange:
		final MultisigTestContext context = new MultisigTestContext();
		final Account multisig = Utils.generateRandomAccount();
		final Account cosignatory = Utils.generateRandomAccount();
		final Account newCosignatory = Utils.generateRandomAccount();
		context.addState(multisig);
		context.addState(cosignatory);
		context.makeCosignatory(cosignatory, multisig, BlockHeight.ONE);

		final Transaction transaction = new MultisigSignerModificationTransaction(
				TimeInstant.ZERO,
				multisig,
				Arrays.asList(new MultisigModification(MultisigModificationType.Add, newCosignatory)));
		transaction.sign();

		// Act:
		final ValidationResult result = context.validateNonOperational(transaction);

		// Assert
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
	}

	@Test
	public void multisigAccountCannotIssueMultisigDelModification() {
		// Arrange:
		final MultisigTestContext context = new MultisigTestContext();
		final Account multisig = Utils.generateRandomAccount();
		final Account cosignatory = Utils.generateRandomAccount();
		final Account newCosignatory = Utils.generateRandomAccount();
		context.addState(multisig);
		context.addState(cosignatory);
		context.makeCosignatory(cosignatory, multisig, BlockHeight.ONE);

		// TODO: change this to del
		final Transaction transaction = new MultisigSignerModificationTransaction(
				TimeInstant.ZERO,
				multisig,
				Arrays.asList(new MultisigModification(MultisigModificationType.Del, newCosignatory)));
		transaction.sign();

		// Act:
		final ValidationResult result = context.validateNonOperational(transaction);

		// Assert
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_TRANSACTION_NOT_ALLOWED_FOR_MULTISIG));
	}

	@Test
	public void multisigAccountCanIssueMultisigSignatureIfAlsoIsCosignatory() {
		// Arrange:
		final MultisigTestContext context = new MultisigTestContext();
		final Account deepMultisig = Utils.generateRandomAccount();
		final Account multisig = Utils.generateRandomAccount();
		final Account cosignatory = Utils.generateRandomAccount();
		context.addState(deepMultisig);
		context.addState(multisig);
		context.addState(cosignatory);
		context.makeCosignatory(cosignatory, multisig, BlockHeight.ONE);
		context.makeCosignatory(multisig, deepMultisig, BlockHeight.ONE);

		final Transaction transaction = new MultisigSignatureTransaction(
				TimeInstant.ZERO,
				multisig,
				Utils.generateRandomHash());
		transaction.sign();

		// Act:
		final ValidationResult result = context.validateNonOperational(transaction);

		// Assert
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
	}

}
