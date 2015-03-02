package org.nem.nis.dao;

import org.hibernate.*;
import org.hibernate.type.LongType;
import org.nem.core.crypto.Hash;
import org.nem.core.model.*;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.nis.dbmodel.*;
import org.nem.nis.mappers.TransactionRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Repository
public class TransferDaoImpl implements TransferDao {
	private final SessionFactory sessionFactory;

	/**
	 * Creates a transfer dao implementation.
	 *
	 * @param sessionFactory The session factory.
	 */
	@Autowired(required = true)
	public TransferDaoImpl(final SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	private Session getCurrentSession() {
		return this.sessionFactory.getCurrentSession();
	}

	// TODO 20150302 BR -> J, G: this is old and we are not using it. It will never ever be fast enough to be of any use. Can we remove it?
	// NOTE: this query will also ask for accounts of senders and recipients!
	@Override
	@Transactional(readOnly = true)
	public Collection<TransferBlockPair> getTransactionsForAccount(final Account address, final Integer timeStamp, final int limit) {
		final Query query = this.getCurrentSession()
				.createQuery("select t, t.block from DbTransferTransaction t " +
						"where t.timeStamp <= :timeStamp AND (t.recipient.printableKey = :pubkey OR t.sender.printableKey = :pubkey) " +
						"order by t.timeStamp desc")
				.setParameter("timeStamp", timeStamp)
				.setParameter("pubkey", address.getAddress().getEncoded())
				.setMaxResults(limit);
		return executeQuery(query);
	}

	@Override
	@Transactional(readOnly = true)
	public Collection<TransferBlockPair> getTransactionsForAccountUsingHash(
			final Account address,
			final Hash hash,
			final BlockHeight height,
			final TransferType transferType,
			final int limit) {
		final Long accountId = this.getAccountId(address);
		if (null == accountId) {
			return new ArrayList<>();
		}

		long maxId = null == hash ? Long.MAX_VALUE : this.getTransactionDescriptorUsingHash(hash, height);
		return this.getTransactionsForAccountUsingId(address, maxId, transferType, limit);
	}

	private Long getTransactionDescriptorUsingHash(
			final Hash hash,
			final BlockHeight height) {
		// since we know the block height and have to search for the hash in all transaction tables, the easiest way to do it
		// is simply to load the complete block from the db. It will be fast enough.
		final BlockLoader blockLoader = new BlockLoader(this.sessionFactory);
		final List<DbBlock> dbBlocks = blockLoader.loadBlocks(height, height);
		if (dbBlocks.isEmpty()) {
			throw new MissingResourceException("transaction not found in the db", Hash.class.toString(), hash.toString());
		}

		final DbBlock dbBlock = dbBlocks.get(0);
		for (final TransactionRegistry.Entry<AbstractBlockTransfer, ?> entry : TransactionRegistry.iterate()) {
			final List<AbstractBlockTransfer> transfers = entry.getFromBlock.apply(dbBlock).stream()
					.filter(t -> t.getTransferHash().equals(hash))
					.collect(Collectors.toList());
			if (!transfers.isEmpty()) {
				return transfers.get(0).getId();
			}
		}

		throw new MissingResourceException("transaction not found in the db", Hash.class.toString(), hash.toString());
	}

	@Override
	@Transactional(readOnly = true)
	public Collection<TransferBlockPair> getTransactionsForAccountUsingId(
			final Account address,
			final Long id,
			final TransferType transferType,
			final int limit) {
		final Long accountId = this.getAccountId(address);
		if (null == accountId) {
			return new ArrayList<>();
		}

		final long maxId = null == id ? Long.MAX_VALUE : id;
		return this.getTransactionsForAccountUpToTransaction(accountId, maxId, limit, transferType);
	}

	private Long getAccountId(final Account account) {
		final Address address = account.getAddress();
		final Query query = this.getCurrentSession()
				.createSQLQuery("select id as accountId from accounts WHERE printablekey=:address")
				.addScalar("accountId", LongType.INSTANCE)
				.setParameter("address", address.getEncoded());
		return (Long)query.uniqueResult();
	}

	private Collection<TransferBlockPair> getTransactionsForAccountUpToTransaction(
			final Long accountId,
			final long maxId,
			final int limit,
			final TransferType transferType) {
		if (TransferType.ALL == transferType) {
			// note that we have to do separate queries for incoming and outgoing transactions since otherwise h2
			// is not able to use an index to speed up the query.
			final Collection<TransferBlockPair> pairs =
					this.getTransactionsForAccountUpToTransactionWithTransferType(accountId, maxId, limit, TransferType.INCOMING);
			pairs.addAll(this.getTransactionsForAccountUpToTransactionWithTransferType(accountId, maxId, limit, TransferType.OUTGOING));
			return this.sortAndLimit(pairs, limit);
		} else {
			final Collection<TransferBlockPair> pairs = this.getTransactionsForAccountUpToTransactionWithTransferType(
					accountId,
					maxId,
					limit,
					transferType);
			return this.sortAndLimit(pairs, limit);
		}
	}

	private Collection<TransferBlockPair> getTransactionsForAccountUpToTransactionWithTransferType(
			final Long accountId,
			final long maxId,
			final int limit,
			final TransferType transferType) {
		final Collection<TransferBlockPair> pairs = new ArrayList<>();
		for (final TransactionRegistry.Entry<?, ?> entry : TransactionRegistry.iterate()) {
			pairs.addAll(entry.getTransactionRetriever.get().getTransfersForAccount(this.getCurrentSession(), accountId, maxId, limit, transferType));
		}

		return pairs;
	}

	@SuppressWarnings("unchecked")
	private static List<TransferBlockPair> executeQuery(final Query q) {
		final List<Object[]> list = q.list();
		return list.stream().map(o -> new TransferBlockPair((AbstractBlockTransfer)o[0], (DbBlock)o[1])).collect(Collectors.toList());
	}

	private Collection<TransferBlockPair> sortAndLimit(final Collection<TransferBlockPair> pairs, final int limit) {
		final List<TransferBlockPair> list = pairs.stream()
				.sorted()
				.collect(Collectors.toList());
		TransferBlockPair curPair = null;
		final Collection<TransferBlockPair> result = new ArrayList<>();
		for (final TransferBlockPair pair : list) {
			if (null == curPair || !(curPair.getTransfer().getId().equals(pair.getTransfer().getId()))) {
				result.add(pair);
				if (limit == result.size()) {
					break;
				}
			}
			curPair = pair;
		}

		return result;
	}
}
