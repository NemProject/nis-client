package org.nem.nis.test;

import org.nem.core.model.Address;
import org.nem.nis.dao.AccountDao;
import org.nem.nis.dbmodel.Account;

import java.util.*;

/**
 * A mock AccountDao implementation.
 */
public class MockAccountDao implements AccountDao {

	private int numGetAccountByPrintableAddressCalls;
	private final Map<String, Account> knownAccounts = new HashMap<>();

	/**
	 * Gets the number of times getAccountByPrintableAddress was called.
	 *
	 * @return The number of times getAccountByPrintableAddress was called.
	 */
	public int getNumGetAccountByPrintableAddressCalls() {
		return this.numGetAccountByPrintableAddressCalls;
	}

	/**
	 * Adds a mapping between a model address and a db-model account.
	 *
	 * @param address   The model address
	 * @param dbAccount The db-model account.
	 */
	public void addMapping(final Address address, final org.nem.nis.dbmodel.Account dbAccount) {
		this.knownAccounts.put(address.getEncoded(), dbAccount);
	}

	/**
	 * Adds a mapping between a model account and a db-model account.
	 *
	 * @param account   The model account
	 * @param dbAccount The db-model account.
	 */
	public void addMapping(final org.nem.core.model.Account account, final org.nem.nis.dbmodel.Account dbAccount) {
		this.addMapping(account.getAddress(), dbAccount);
	}

	@Override
	public org.nem.nis.dbmodel.Account getAccount(final Long id) {
		throw new UnsupportedOperationException();
	}

	@Override
	public org.nem.nis.dbmodel.Account getAccountByPrintableAddress(final String printableAddress) {
		++this.numGetAccountByPrintableAddressCalls;
		return this.knownAccounts.get(printableAddress);
	}

	@Override
	public void save(final org.nem.nis.dbmodel.Account account) {
		throw new UnsupportedOperationException();
	}
}