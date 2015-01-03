package org.nem.nis.validators;

import org.nem.core.time.TimeProvider;
import org.nem.nis.cache.*;
import org.nem.nis.poi.PoiOptions;

import java.util.function.Consumer;

/**
 * Factory for creating TransactionValidator objects.
 */
public class TransactionValidatorFactory {
	private final TimeProvider timeProvider;
	private final PoiOptions poiOptions;

	/**
	 * Creates a new factory.
	 *
	 * @param timeProvider The time provider.
	 * @param poiOptions The poi options.
	 */
	public TransactionValidatorFactory(
			final TimeProvider timeProvider,
			final PoiOptions poiOptions) {
		this.timeProvider = timeProvider;
		this.poiOptions = poiOptions;
	}

	/**
	 * Creates a transaction validator that only contains single validators.
	 *
	 * @param accountStateCache The account state cache.
	 * @return The validator.
	 */
	public SingleTransactionValidator createSingle(final ReadOnlyAccountStateCache accountStateCache) {
		return this.createSingleBuilder(accountStateCache).build();
	}

	/**
	 * Creates a transaction validator builder only that is initialized with single validators.
	 *
	 * @param accountStateCache The account state cache.
	 * @return The builder.
	 */
	public AggregateSingleTransactionValidatorBuilder createSingleBuilder(final ReadOnlyAccountStateCache accountStateCache) {
		final AggregateSingleTransactionValidatorBuilder builder = new AggregateSingleTransactionValidatorBuilder();
		this.visitSingleSubValidators(builder::add, accountStateCache);
		return builder;
	}

	/**
	 * Creates a transaction validator that only contains batch validators.
	 *
	 * @param transactionHashCache The transaction hash cache.
	 * @return The validator.
	 */
	public BatchTransactionValidator createBatch(final ReadOnlyHashCache transactionHashCache) {
		final AggregateBatchTransactionValidatorBuilder builder = new AggregateBatchTransactionValidatorBuilder();
		this.visitBatchSubValidators(builder::add, transactionHashCache);
		return builder.build();
	}

	/**
	 * Visits all sub validators that comprise the validator returned by createSingle.
	 *  @param visitor The visitor.
	 * @param accountStateCache The account state cache.
	 */
	public void visitSingleSubValidators(
			final Consumer<SingleTransactionValidator> visitor,
			final ReadOnlyAccountStateCache accountStateCache) {
		visitor.accept(new UniversalTransactionValidator());
		visitor.accept(new MultisigNonOperationalValidator(accountStateCache));
		visitor.accept(new NonFutureEntityValidator(this.timeProvider));
		visitor.accept(new TransferTransactionValidator());
		visitor.accept(new ImportanceTransferTransactionValidator(accountStateCache, this.poiOptions.getMinHarvesterBalance()));

		visitor.accept(new MultisigTransactionSignerValidator(accountStateCache));
		visitor.accept(new MultisigSignerModificationTransactionValidator(accountStateCache));
	}

	/**
	 * Visits all sub validators that comprise the validator returned by createBatch.
	 *
	 * @param visitor The visitor.
	 * @param transactionHashCache The transaction hash cache.
	 */
	public void visitBatchSubValidators(
			final Consumer<BatchTransactionValidator> visitor,
			final ReadOnlyHashCache transactionHashCache) {
		visitor.accept(new BatchUniqueHashTransactionValidator(transactionHashCache));
	}
}
