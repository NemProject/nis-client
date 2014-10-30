package org.nem.nis.service;

import org.nem.core.crypto.Hash;
import org.nem.core.model.*;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.nis.dao.TransferDao;
import org.nem.nis.dbmodel.Transfer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class RequiredTransferDaoAdapter implements RequiredTransferDao {

	private final TransferDao transferDao;

	@Autowired(required = true)
	public RequiredTransferDaoAdapter(final TransferDao transferDao) {
		this.transferDao = transferDao;
	}

	@Override
	public Long count() {
		return this.transferDao.count();
	}

	@Override
	public Transfer findByHash(final byte[] txHash) {
		final Transfer transfer = this.transferDao.findByHash(txHash);
		if (null == transfer) {
			throw createMissingResourceException(txHash.toString());
		}
		return transfer;
	}

	// TODO 20140927 J-G,B: we might want to revisit this class since we're only using a few of the functions of it
	// > that's probably a good indication that this class shouldn't implement ReadOnlyTransferDao
	// > or at least add tests for the following functions (eventually)

	@Override
	public Transfer findByHash(final byte[] txHash, final long maxBlockHeight) {
		final Transfer transfer = this.transferDao.findByHash(txHash, maxBlockHeight);
		if (null == transfer) {
			throw createMissingResourceException(txHash.toString());
		}
		return transfer;
	}

	@Override
	public boolean duplicateHashExists(Collection<Hash> hashes, BlockHeight maxBlockHeight) {
		return this.transferDao.duplicateHashExists(hashes, maxBlockHeight);
	}

	//TODO: we should probably delegate in getTransactionsForAccount too and add tests for delegation

	@Override
	public Collection<Object[]> getTransactionsForAccount(final Account account, final Integer timeStamp, final int limit) {
		final Collection<Object[]> transfers = this.transferDao.getTransactionsForAccount(account, timeStamp, limit);
		// TODO: throw exception
		return transfers;
	}

	@Override
	public Collection<Object[]> getTransactionsForAccountUsingHash(final Account account, final Hash hash, final TransferType transferType, final int limit) {
		// this can throw
		final Collection<Object[]> transfers = this.transferDao.getTransactionsForAccountUsingHash(account, hash, transferType, limit);
		return transfers;
	}

	private static MissingResourceException createMissingResourceException(final String key) {
		return new MissingResourceException("block not found in the db", Block.class.getName(), key);
	}
}
