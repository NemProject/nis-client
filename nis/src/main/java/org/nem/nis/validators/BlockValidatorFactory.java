package org.nem.nis.validators;

import org.nem.core.time.TimeProvider;
import org.nem.nis.poi.PoiFacade;

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
	 * @param poiFacade The poi facade.
	 * @return The validator.
	 */
	public BlockValidator create(final PoiFacade poiFacade) {
		return create(new AggregateBlockValidatorBuilder(), poiFacade);
	}

	/**
	 * Creates a block validator.
	 * TODO why did this signature change?
	 *
	 * @param builder The aggregate block validator builder.
	 * @param poiFacade The poi facade.
	 * @return The validator.
	 */
	public BlockValidator create(final AggregateBlockValidatorBuilder builder, final PoiFacade poiFacade) {
		builder.add(new NonFutureEntityValidator(this.timeProvider));
		builder.add(new TransactionDeadlineBlockValidator());
		builder.add(new EligibleSignerBlockValidator(poiFacade));
		builder.add(new MaxTransactionsBlockValidator());
		builder.add(new NoSelfSignedTransactionsBlockValidator(poiFacade));
		builder.add(new BlockImportanceTransferValidator());
		builder.add(new BlockImportanceTransferBalanceValidator());
		return builder.build();
	}
}
