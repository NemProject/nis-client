package org.nem.nis.dao;

import org.nem.core.crypto.Hash;
import org.nem.core.model.Account;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.nis.dbmodel.*;

import java.util.Collection;

/**
 * Read-only DAO for accessing DbTransferTransaction objects.
 */
public interface ReadOnlyTransferDao extends SimpleReadOnlyTransferDao<DbTransferTransaction> {
	/*
	 * Types of transfers that can be requested.
	 */
	public enum TransferType {
		ALL,
		INCOMING,
		OUTGOING
	}

	/**
	 * Retrieves limit Transfers from db for given account.
	 *
	 * @param account The account.
	 * @param timeStamp The maximum timestamp of a transfer.
	 * @param limit The limit.
	 * @return Collection of transfer block pairs.
	 */
	public Collection<TransferBlockPair> getTransactionsForAccount(final Account account, final Integer timeStamp, final int limit);

	/**
	 * Retrieves limit Transfers from db for given account.
	 *
	 * @param account The account.
	 * @param hash The hash of "top-most" transfer.
	 * @param height The block height at which to search for the hash.
	 * @param transferType Type of returned transfers.
	 * @param limit The limit.
	 * @return Collection of transfer block pairs.
	 */
	public Collection<TransferBlockPair> getTransactionsForAccountUsingHash(
			final Account account,
			final Hash hash,
			final BlockHeight height,
			final TransferType transferType,
			final int limit);

	/**
	 * Retrieves limit Transfers from db for given account.
	 * These transfers can by of any type.
	 *
	 * @param account The account.
	 * @param id The id of "top-most" transfer.
	 * @param transferType Type of returned transfers.
	 * @param limit The limit.
	 * @return Collection of transfer block pairs.
	 */
	public Collection<TransferBlockPair> getTransactionsForAccountUsingId(
			final Account account,
			final Long id,
			final TransferType transferType,
			final int limit);

	/**
	 * Retrieves limit Transfers from db for given account.
	 *
	 * @param accountId The account is.
	 * @param maxId The id of "top-most" transfer.
	 * @param limit The limit.
	 * @param transferType Type of returned transfers.
	 * @return Collection of transfer block pairs.
	 */
	public Collection<TransferBlockPair> getTransfersForAccount(
			final long accountId,
			final long maxId,
			final int limit,
			final TransferType transferType);

	public Collection<TransferBlockPair> getImportanceTransfersForAccount(
			final long accountId,
			final long maxId,
			final int limit,
			final TransferType transferType);

	public Collection<TransferBlockPair> getMultisigSignerModificationsForAccount(
			final long accountId,
			final long maxId,
			final int limit,
			final TransferType transferType);

	public Collection<TransferBlockPair> getMultisigTransactionsForAccount(
			final long accountId,
			final long maxId,
			final int limit,
			final TransferType transferType);
}
