package org.nem.nis.validators.transaction;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.model.*;
import org.nem.core.model.mosaic.Mosaic;
import org.nem.core.model.namespace.*;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.test.Utils;
import org.nem.core.time.TimeInstant;
import org.nem.nis.cache.*;
import org.nem.nis.test.DebitPredicates;
import org.nem.nis.validators.ValidationContext;

public class MosaicCreationTransactionValidatorTest {
	private static final Account SIGNER = Utils.generateRandomAccount();
	private static final BlockHeight VALIDATION_HEIGHT = new BlockHeight(21);

	//region valid

	@Test
	public void validTransactionValidates() {
		// Arrange:
		final TestContext context = new TestContext();
		context.activateNamespaceAtHeight(SIGNER, VALIDATION_HEIGHT);
		final MosaicCreationTransaction transaction = createTransaction();

		// Act:
		final ValidationResult result = context.validate(transaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.SUCCESS));
	}

	//endregion

	//region invalid

	@Test
	public void transactionIsInvalidIfNamespaceIsInactive() {
		// Arrange:
		final TestContext context = new TestContext();
		context.activateNamespaceAtHeight(SIGNER, VALIDATION_HEIGHT.next());
		final MosaicCreationTransaction transaction = createTransaction();

		// Act:
		final ValidationResult result = context.validate(transaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_NAMESPACE_EXPIRED));
	}

	@Test
	public void transactionIsInvalidIfNamespaceIsNotOwned() {
		// Arrange:
		final TestContext context = new TestContext();
		context.activateNamespaceAtHeight(Utils.generateRandomAccount(), VALIDATION_HEIGHT);
		final MosaicCreationTransaction transaction = createTransaction();

		// Act:
		final ValidationResult result = context.validate(transaction);

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(ValidationResult.FAILURE_NAMESPACE_OWNER_CONFLICT));
	}

	//endregion

	private static MosaicCreationTransaction createTransaction() {
		final Mosaic mosaic = Utils.createMosaic(SIGNER);
		return new MosaicCreationTransaction(TimeInstant.ZERO, SIGNER, mosaic);
	}

	private static class TestContext {
		final NamespaceCache namespaceCache = new DefaultNamespaceCache();
		final MosaicCache mosaicCache = new DefaultMosaicCache();
		final MosaicCreationTransactionValidator validator = new MosaicCreationTransactionValidator(this.namespaceCache, this.mosaicCache);

		public void activateNamespaceAtHeight(final Account signer, final BlockHeight height) {
			this.namespaceCache.add(new Namespace(new NamespaceId("alice"), signer, height));
			this.namespaceCache.add(new Namespace(new NamespaceId("alice.vouchers"), signer, height));
		}

		public ValidationResult validate(final MosaicCreationTransaction transaction) {
			return this.validator.validate(transaction, new ValidationContext(VALIDATION_HEIGHT, DebitPredicates.Throw));
		}
	}
}