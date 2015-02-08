package org.nem.nis.validators;

import org.nem.core.model.*;

/**
 * SingleTransactionValidator decorator that knows how to validate child transactions.
 */
public class ChildAwareSingleTransactionValidator implements SingleTransactionValidator {
	private final SingleTransactionValidator validator;

	/**
	 * Creates a child-aware single transaction validator.
	 *
	 * @param validator The decorated validator.
	 */
	public ChildAwareSingleTransactionValidator(final SingleTransactionValidator validator) {
		this.validator = validator;
	}

	@Override
	public ValidationResult validate(final Transaction transaction, final ValidationContext context) {
		return ValidationResult.aggregate(
				TransactionExtensions.streamDefault(transaction)
						.map(t -> this.validator.validate(t, context))
						.iterator());
	}
}
