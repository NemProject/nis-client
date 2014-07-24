package org.nem.nis.test;

import org.nem.core.model.Block;
import org.nem.core.serialization.AccountLookup;
import org.nem.nis.*;
import org.nem.nis.dao.*;
import org.nem.nis.service.BlockChainLastBlockLayer;

public class MockForaging extends Foraging {
	private int removeFromUnconfirmedTransactionsCalls = 0;

	// final AccountLookup accountLookup, final BlockDao blockDao, final BlockChainLastBlockLayer blockChainLastBlockLayer, final TransferDao transferDao) {
	public MockForaging(final AccountLookup accountLookup, final BlockDao blockDao, final BlockChainLastBlockLayer blockChainLastBlockLayer, final TransferDao transferDao) {
		super(accountLookup, blockDao, blockChainLastBlockLayer, transferDao);
	}

	public MockForaging(AccountAnalyzer accountAnalyzer, BlockChainLastBlockLayer lastBlockLayer) {
		this(accountAnalyzer, new MockBlockDao(null), lastBlockLayer, new MockTransferDaoImpl());
	}

	@Override
	public void removeFromUnconfirmedTransactions(Block block) {
		this.removeFromUnconfirmedTransactionsCalls++;
	}
}
