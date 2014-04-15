package org.nem.nis.mappers;

import org.nem.core.crypto.Signature;
import org.nem.nis.dbmodel.Transfer;
import org.nem.core.model.*;
import org.nem.core.serialization.AccountLookup;
import org.nem.core.time.TimeInstant;
import org.nem.core.transactions.TransferTransaction;
import org.nem.core.utils.ByteUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Static class that contains functions for converting to and from
 * db-model Block and model Block.
 */
public class BlockMapper {

	/**
	 * Converts a Block model to a Block db-model.
	 *
	 * @param block      The block model.
	 * @param accountDao The account dao lookup object.
	 *
	 * @return The Block db-model.
	 */
	public static org.nem.nis.dbmodel.Block toDbModel(final Block block, final AccountDaoLookup accountDao) {
		final org.nem.nis.dbmodel.Account forager = accountDao.findByAddress(block.getSigner().getAddress());

		final Hash blockHash = HashUtils.calculateHash(block);
		final org.nem.nis.dbmodel.Block dbBlock = new org.nem.nis.dbmodel.Block(
				blockHash,
				block.getVersion(),
				block.getPreviousBlockHash(),
				block.getTimeStamp().getRawTime(),
				forager,
				block.getSignature().getBytes(),
				block.getHeight(),
				0L,
				block.getTotalFee().getNumMicroNem(),
				block.getDifficulty());

		int i = 0;
		final List<Transfer> transactions = new ArrayList<>(block.getTransactions().size());
		for (final Transaction transaction : block.getTransactions()) {
			final Transfer dbTransfer = TransferMapper.toDbModel((TransferTransaction)transaction, i++, accountDao);
			dbTransfer.setBlock(dbBlock);
			transactions.add(dbTransfer);
		}

		dbBlock.setBlockTransfers(transactions);
		return dbBlock;
	}

	/**
	 * Converts a Block db-model to a Block model.
	 *
	 * @param dbBlock       The block db-model.
	 * @param accountLookup The account lookup object.
	 *
	 * @return The Block model.
	 */
	public static Block toModel(final org.nem.nis.dbmodel.Block dbBlock, final AccountLookup accountLookup) {
		final Address foragerAddress = Address.fromPublicKey(dbBlock.getForger().getPublicKey());
		final Account forager = accountLookup.findByAddress(foragerAddress);

		final Block block = new org.nem.core.model.Block(
				forager,
				dbBlock.getPrevBlockHash(),
				new TimeInstant(dbBlock.getTimestamp()),
				dbBlock.getHeight());

		Long difficulty = dbBlock.getDifficulty();
		if (difficulty == null) {
			difficulty = 0L;
		}
		block.setDifficulty(difficulty);

		block.setSignature(new Signature(dbBlock.getForgerProof()));
		for (final Transfer dbTransfer : dbBlock.getBlockTransfers()) {
			final TransferTransaction transfer = TransferMapper.toModel(dbTransfer, accountLookup);
			block.addTransaction(transfer);
		}

		return block;
	}
}