package org.nem.nis.validators;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.*;
import org.nem.nis.test.MultisigTestContext;

public class MultisigAggregateModificationTransactionValidatorTest {
	@Test
	public void canValidateOtherTransactions() {
		// Arrange:
		final MultisigTestContext context = new MultisigTestContext();
		final Transaction transaction = Mockito.mock(Transaction.class);

		// Act:
		final ValidationResult result = context.validateMultisigModification(transaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
	}

	@Test
	public void addingNewCosignatoryIsValid() {
		// Arrange:
		final MultisigTestContext context = new MultisigTestContext();
		final Transaction transaction = context.createMultisigModificationTransaction(MultisigModificationType.Add);

		// Act:
		final ValidationResult result = context.validateMultisigModification(transaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
	}

	@Test
	public void addingExistingCosignatoryIsInvalid() {
		// Arrange:
		final MultisigTestContext context = new MultisigTestContext();
		// TODO 20150103 J-G: so i understand, the context is returning a transaction that will make context.signer a cosignatory of context.multisig?
		final Transaction transaction = context.createMultisigModificationTransaction(MultisigModificationType.Add);
		context.makeCosignatory(context.signer, context.multisig);

		// Act:
		final ValidationResult result = context.validateMultisigModification(transaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_MULTISIG_ALREADY_A_COSIGNER));
	}

	@Test
	public void removingNonExistingCosignatoryIsInvalid() {
		// Arrange:
		final MultisigTestContext context = new MultisigTestContext();
		final Transaction transaction = context.createMultisigModificationTransaction(MultisigModificationType.Del);

		// Act:
		final ValidationResult result = context.validateMultisigModification(transaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_MULTISIG_NOT_A_COSIGNER));
	}

	@Test
	public void removingExistingCosignatoryIsValid() {
		// Arrange:
		final MultisigTestContext context = new MultisigTestContext();
		final Transaction transaction = context.createMultisigModificationTransaction(MultisigModificationType.Del);
		context.makeCosignatory(context.signer, context.multisig);

		// Act:
		final ValidationResult result = context.validateMultisigModification(transaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
	}
}
