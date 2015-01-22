package org.nem.nis.validators;

import org.nem.core.model.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * A block validator that validates:
 * - A block contains no more than one AggregateMultisigModification affecting any multisig account.
 */
public class BlockMultisigAggregateModificationValidator implements BlockValidator {
	@Override
	public ValidationResult validate(final Block block) {
		final List<Account> modificationSigners = BlockExtensions.streamDefault(block)
				.filter(t -> TransactionTypes.MULTISIG_AGGREGATE_MODIFICATION == t.getType())
				.map(t -> t.getSigner())
				.collect(Collectors.toList());

		final Set<Account> uniqueModificationSigners = new HashSet<>(modificationSigners);
		return modificationSigners.size() == uniqueModificationSigners.size()
				? ValidationResult.SUCCESS
				: ValidationResult.FAILURE_CONFLICTING_MULTISIG_MODIFICATION;
	}
}
