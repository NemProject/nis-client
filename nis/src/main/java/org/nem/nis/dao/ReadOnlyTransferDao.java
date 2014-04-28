package org.nem.nis.dao;

import org.nem.core.model.Account;
import org.nem.nis.dbmodel.Transfer;

import java.util.Collection;

/**
 * Read-only DAO for accessing db Transfer objects.
 */
public interface ReadOnlyTransferDao {
	/**
	 * Returns number of transfers in the database.
	 * <p/>
	 * Note: this  will return number of transactions of Transfer type only.
	 *
	 * @return number of transfers in the database.
	 */
	public Long count();

	/**
	 * Retrieves Transfer from db given it's hash.
	 *
	 * @param txHash hash of a transfer to retrieve.
	 *
	 * @return Transfer having given hash or null.
	 */
	public Transfer findByHash(byte[] txHash);

	/**
	 * Retrieves latest limit Transfers from db for given account
	 *
	 * @param account The account.
	 * @param limit The limit.
	 * @return (sorted?) Collection of Transfers
	 */
	public Collection<Transfer> getTransactionsForAccount(final Account account, final int limit);
}
