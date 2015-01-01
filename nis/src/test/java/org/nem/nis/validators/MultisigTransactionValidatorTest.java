package org.nem.nis.validators;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.*;
import org.nem.core.model.primitive.*;
import org.nem.core.test.Utils;
import org.nem.core.time.TimeInstant;
import org.nem.nis.BlockMarkerConstants;
import org.nem.nis.cache.AccountStateCache;
import org.nem.nis.state.AccountState;
import org.nem.nis.test.MultisigTestContext;

public class MultisigTransactionValidatorTest {
	private static final BlockHeight TEST_HEIGHT = new BlockHeight(BlockMarkerConstants.BETA_MULTISIG_FORK);

	@Test
	public void validatorCanValidateOtherTransactions() {
		// Arrange:
		final MultisigTestContext context = new MultisigTestContext();
		final Transaction transaction = Mockito.mock(Transaction.class);

		// Act:
		final ValidationResult result = context.validateMultisigTransaction(transaction, BlockHeight.ONE);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
	}

	@Test
	public void multisigTransactionDoesNotValidateIfSignerIsNotCosignatory() {
		// Arrange:
		final MultisigTestContext context = new MultisigTestContext();
		final Transaction transaction = context.createMultisigTransferTransaction();

		// Act:
		final ValidationResult result = context.validateMultisigTransaction(transaction, TEST_HEIGHT);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_MULTISIG_NOT_A_COSIGNER));
	}

	@Test
	public void multisigTransactionValidatesIfSignerIsCosignatory() {
		// Arrange:
		final MultisigTestContext context = new MultisigTestContext();
		final Transaction transaction = context.createMultisigTransferTransaction();
		context.makeCosignatory(context.signer, context.multisig, BlockHeight.ONE);

		// Act:
		final ValidationResult result = context.validateMultisigTransaction(transaction, TEST_HEIGHT);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
	}

	@Test
	public void multisigTransactionDoesNotValidateBelowForkBLock() {
		// Arrange:
		final MultisigTestContext context = new MultisigTestContext();
		final Transaction transaction = context.createMultisigTransferTransaction();
		context.makeCosignatory(context.signer, context.multisig, BlockHeight.ONE);

		// Act:
		final ValidationResult result = context.validateMultisigTransaction(transaction, TEST_HEIGHT.prev());

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_ENTITY_UNUSABLE));
	}
}
