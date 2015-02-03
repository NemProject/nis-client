package org.nem.nis.sync;

import org.nem.core.model.Account;
import org.nem.core.model.primitive.Amount;
import org.nem.nis.cache.ReadOnlyAccountStateCache;
import org.nem.nis.state.ReadOnlyAccountInfo;
import org.nem.nis.validators.DebitPredicate;

/**
 * A default debit predicate implementation.
 */
public class DefaultDebitPredicate implements DebitPredicate {
	private final ReadOnlyAccountStateCache accountStateCache;

	/**
	 * Creates a default debit predicate.
	 *
	 * @param accountStateCache The account state cache.
	 */
	public DefaultDebitPredicate(final ReadOnlyAccountStateCache accountStateCache) {
		this.accountStateCache = accountStateCache;
	}

	@Override
	public boolean canDebit(final Account account, final Amount amount) {
		final ReadOnlyAccountInfo accountInfo = this.accountStateCache.findStateByAddress(account.getAddress()).getAccountInfo();
		return accountInfo.getBalance().compareTo(amount) >= 0;
	}
}