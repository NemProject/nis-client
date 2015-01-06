package org.nem.nis.dao;

import org.nem.nis.dbmodel.DbAccount;

/**
 * DAO for accessing DbAccount objects
 */
public interface AccountDao {
	/**
	 * Retrieves DbAccount from db given it's id in the database.
	 *
	 * @param id id of a record.
	 * @return associated DbAccount or null if there isn't DbAccount with such id.
	 */
	public DbAccount getAccount(Long id);

	/**
	 * Retrieves DbAccount from db given it's printable (encoded) address.
	 *
	 * @param printableAddress NEM address
	 * @return DbAccount associated with given printableAddress or null.
	 */
	public DbAccount getAccountByPrintableAddress(String printableAddress);

	/**
	 * Saves an account in the database.
	 * Note: if id wasn't set, it'll be filled after save()
	 *
	 * @param account DbAccount that's going to be saved.
	 */
	public void save(DbAccount account);
}
