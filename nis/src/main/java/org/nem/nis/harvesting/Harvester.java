package org.nem.nis.harvesting;

import org.nem.core.model.*;
import org.nem.core.time.*;
import org.nem.nis.dbmodel.DbBlock;
import org.nem.nis.mappers.NisDbModelToModelMapper;
import org.nem.nis.service.BlockChainLastBlockLayer;

public class Harvester {
	private final TimeProvider timeProvider;
	private final BlockChainLastBlockLayer blockChainLastBlockLayer;
	private final UnlockedAccounts unlockedAccounts;
	private final NisDbModelToModelMapper mapper;
	private final BlockGenerator generator;

	/**
	 * Creates a new harvester.
	 *
	 * @param timeProvider The time provider.
	 * @param blockChainLastBlockLayer The block chain last block layer.
	 * @param unlockedAccounts The unlocked accounts.
	 * @param mapper The mapper.
	 * @param generator The block generator.
	 */
	public Harvester(
			final TimeProvider timeProvider,
			final BlockChainLastBlockLayer blockChainLastBlockLayer,
			final UnlockedAccounts unlockedAccounts,
			final NisDbModelToModelMapper mapper,
			final BlockGenerator generator) {
		this.timeProvider = timeProvider;
		this.blockChainLastBlockLayer = blockChainLastBlockLayer;
		this.unlockedAccounts = unlockedAccounts;
		this.mapper = mapper;
		this.generator = generator;
	}

	/**
	 * Returns harvested block or null.
	 *
	 * @return Best block that could be created by unlocked accounts.
	 */
	public Block harvestBlock() {
		final TimeInstant blockTime = this.timeProvider.getCurrentTime();
		if (this.blockChainLastBlockLayer.isLoading() || this.unlockedAccounts.size() == 0) {
			return null;
		}

		final DbBlock dbLastBlock = this.blockChainLastBlockLayer.getLastDbBlock();
		final Block lastBlock = this.mapper.map(dbLastBlock);

		GeneratedBlock bestGeneratedBlock = null;
		for (final Account harvester : this.unlockedAccounts) {
			if (!this.generator.isAllowedToGenerateNewBlock(lastBlock, harvester, blockTime)) {
				continue;
			}

			final GeneratedBlock generatedBlock = this.generator.generateNextBlock(
					lastBlock,
					harvester,
					blockTime);

			if (null != generatedBlock && (null == bestGeneratedBlock || generatedBlock.getScore() > bestGeneratedBlock.getScore())) {
				bestGeneratedBlock = generatedBlock;
			}
		}

		return null == bestGeneratedBlock ? null : bestGeneratedBlock.getBlock();
	}
}
