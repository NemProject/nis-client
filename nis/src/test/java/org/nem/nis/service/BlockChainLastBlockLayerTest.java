package org.nem.nis.service;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.crypto.Signature;
import org.nem.core.model.Account;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.test.Utils;
import org.nem.core.time.TimeInstant;
import org.nem.nis.dbmodel.Block;
import org.nem.nis.mappers.*;
import org.nem.nis.test.*;

public class BlockChainLastBlockLayerTest {
	@Test
	public void unintializedLastBlockLayerReturnsNull() {
		// Act:
		final BlockChainLastBlockLayer lastBlockLayer = this.createBlockChainLastBlockLayer();

		// Assert:
		Assert.assertNull(lastBlockLayer.getLastDbBlock());
	}

	@Test
	public void afterAnalyzeLastBlockLayerReturnsBlock() {
		// Arrange:
		final BlockChainLastBlockLayer lastBlockLayer = this.createBlockChainLastBlockLayer();
		final Block block = this.createDbBlock(1);

		// Act:
		lastBlockLayer.analyzeLastBlock(block);

		// Assert:
		Assert.assertThat(lastBlockLayer.getLastDbBlock(), IsSame.sameInstance(block));
	}

	@Test
	public void foo() {
		// Arrange:
		final MockAccountDao accountDao = new MockAccountDao();
		final MockBlockDao mockBlockDao = new MockBlockDao(null);
		final BlockChainLastBlockLayer lastBlockLayer = new BlockChainLastBlockLayer(accountDao, mockBlockDao);
		final org.nem.core.model.Block lastBlock = createBlock();
		final Block lastDbBlock = BlockMapper.toDbModel(lastBlock, new AccountDaoLookupAdapter(accountDao));

		// Act:
		lastBlockLayer.analyzeLastBlock(lastDbBlock);
		final Block result1 = lastBlockLayer.getLastDbBlock();

		final org.nem.core.model.Block nextBlock = createBlock();
		lastBlockLayer.addBlockToDb(nextBlock);

		// Assert:
		Assert.assertThat(result1, IsSame.sameInstance(lastDbBlock));
		final Block last = mockBlockDao.getLastSavedBlock();
		Assert.assertThat(last.getId(), IsEqual.equalTo(1L));
		Assert.assertThat(new Signature(last.getForgerProof()), IsEqual.equalTo(nextBlock.getSignature()));
		Assert.assertThat(lastDbBlock.getNextBlockId(), IsEqual.equalTo(last.getId()));
		Assert.assertThat(lastBlockLayer.getLastDbBlock(), IsEqual.equalTo(last));
	}

	private static org.nem.core.model.Block createBlock(final Account forger) {
		// Arrange:
		final org.nem.core.model.Block block = new org.nem.core.model.Block(forger,
				Utils.generateRandomHash(),
				Utils.generateRandomHash(),
				new TimeInstant(7),
				new BlockHeight(3));
		block.sign();
		return block;
	}

	private static org.nem.core.model.Block createBlock() {
		// Arrange:
		return createBlock(Utils.generateRandomAccount());
	}

	private Block createDbBlock(final long i) {
		final Block block = new Block();
		block.setShortId(i);
		return block;
	}

	private BlockChainLastBlockLayer createBlockChainLastBlockLayer() {
		final MockAccountDao accountDao = new MockAccountDao();
		final MockBlockDao mockBlockDao = new MockBlockDao(null);
		return new BlockChainLastBlockLayer(accountDao, mockBlockDao);
	}
}
