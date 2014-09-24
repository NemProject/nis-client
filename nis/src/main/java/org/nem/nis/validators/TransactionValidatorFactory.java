package org.nem.nis.validators;

import org.nem.core.time.TimeProvider;
import org.nem.nis.dao.*;
import org.nem.nis.poi.PoiFacade;

/**
 * Factory for creating TransactionValidator objects.
 */
public class TransactionValidatorFactory {
	private final TransferDao transferDao;
	private final ImportanceTransferDao importanceTransferDao;
	private final TimeProvider timeProvider;

	/**
	 * Creates a new factory.
	 *
	 * @param transferDao The transfer dao.
	 * @param importanceTransferDao The importance transfer dao.
	 * @param timeProvider The time provider.
	 */
	public TransactionValidatorFactory(
			final TransferDao transferDao,
			final ImportanceTransferDao importanceTransferDao,
			final TimeProvider timeProvider) {
		this.transferDao = transferDao;
		this.importanceTransferDao = importanceTransferDao;
		this.timeProvider = timeProvider;
	}

	/**
	 * Creates a transaction validator.
	 *
	 * @param poiFacade The poi facade.
	 * @return The validator.
	 */
	public TransactionValidator create(final PoiFacade poiFacade) {
		final AggregateTransactionValidatorBuilder builder = new AggregateTransactionValidatorBuilder();
		builder.add(new UniversalTransactionValidator());
		builder.add(new NonFutureEntityValidator(this.timeProvider));
		builder.add(new TransferTransactionValidator());
		builder.add(new ImportanceTransferTransactionValidator(poiFacade));
		builder.add(new UniqueHashTransactionValidator(this.transferDao, this.importanceTransferDao));
		return builder.build();
	}
}
