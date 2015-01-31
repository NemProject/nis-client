package org.nem.nis.validators;

import org.nem.core.time.TimeProvider;
import org.nem.nis.cache.ReadOnlyNisCache;

import java.util.function.Consumer;

/**
 * Factory for creating BlockValidator objects.
 */
public class BlockValidatorFactory {
	private final TimeProvider timeProvider;

	/**
	 * Creates a new factory.
	 *
	 * @param timeProvider The time provider.
	 */
	public BlockValidatorFactory(final TimeProvider timeProvider) {
		this.timeProvider = timeProvider;
	}

	/**
	 * Creates a block validator.
	 *
	 * @param nisCache The NIS cache.
	 * @return The validator.
	 */
	public BlockValidator create(final ReadOnlyNisCache nisCache) {
		final AggregateBlockValidatorBuilder builder = new AggregateBlockValidatorBuilder();
		this.visitSubValidators(builder::add, nisCache);
		return builder.build();
	}

	/**
	 * Visits all sub validators that comprise the validator returned by create.
	 *
	 * @param visitor The visitor.
	 * @param nisCache The NIS cache.
	 */
	public void visitSubValidators(final Consumer<BlockValidator> visitor, final ReadOnlyNisCache nisCache) {
		visitor.accept(new NonFutureEntityValidator(this.timeProvider));
		visitor.accept(new TransactionDeadlineBlockValidator());
		visitor.accept(new EligibleSignerBlockValidator(nisCache.getAccountStateCache()));
		visitor.accept(new MaxTransactionsBlockValidator());
		visitor.accept(new NoSelfSignedTransactionsBlockValidator(nisCache.getAccountStateCache()));
		visitor.accept(new BlockImportanceTransferValidator());
		visitor.accept(new BlockImportanceTransferBalanceValidator());
		visitor.accept(new BlockUniqueHashTransactionValidator(nisCache.getTransactionHashCache()));
		visitor.accept(new BlockMultisigAggregateModificationValidator());
	}
}
