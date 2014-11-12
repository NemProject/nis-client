package org.nem.nis.mappers;

import org.nem.core.crypto.*;
import org.nem.core.model.Account;
import org.nem.core.model.*;
import org.nem.core.model.Block;
import org.nem.core.model.primitive.*;
import org.nem.core.serialization.*;
import org.nem.core.time.TimeInstant;
import org.nem.nis.dbmodel.*;

import java.util.*;

/**
 * Static class that contains functions for converting to and from
 * db-model Block and model Block.
 */
public class BlockMapper {

	/**
	 * Converts a Block model to a Block db-model.
	 *
	 * @param block The block model.
	 * @param accountDao The account dao lookup object.
	 * @return The Block db-model.
	 */
	public static org.nem.nis.dbmodel.Block toDbModel(final Block block, final AccountDaoLookup accountDao) {
		final org.nem.nis.dbmodel.Account harvester = accountDao.findByAddress(block.getSigner().getAddress());
		final org.nem.nis.dbmodel.Account lessor = block.getLessor() != null ? accountDao.findByAddress(block.getLessor().getAddress()) : null;

		final Hash blockHash = HashUtils.calculateHash(block);
		final org.nem.nis.dbmodel.Block dbBlock = new org.nem.nis.dbmodel.Block(
				blockHash,
				block.getVersion(),
				block.getGenerationHash(),
				block.getPreviousBlockHash(),
				block.getTimeStamp().getRawTime(),
				harvester,
				block.getSignature().getBytes(),
				block.getHeight().getRaw(),
				0L,
				block.getTotalFee().getNumMicroNem(),
				block.getDifficulty().getRaw(),
				lessor);

		int i = 0;
		int importanceTransferIndex = 0;
		int transferIndex = 0;
		final List<Transfer> transferTransactions = new ArrayList<>(block.getTransactions().size());
		final List<ImportanceTransfer> importanceTransferTransactions = new ArrayList<>(block.getTransactions().size());
		for (final Transaction transaction : block.getTransactions()) {
			switch (transaction.getType()) {
				case TransactionTypes.TRANSFER: {
					final Transfer dbTransfer = TransferMapper.toDbModel(
							(TransferTransaction)transaction,
							i++,
							importanceTransferIndex++,
							accountDao);
					dbTransfer.setBlock(dbBlock);
					transferTransactions.add(dbTransfer);
				}
				break;
				case TransactionTypes.IMPORTANCE_TRANSFER: {
					final ImportanceTransfer dbTransfer = ImportanceTransferMapper.toDbModel(
							(ImportanceTransferTransaction)transaction,
							i++,
							transferIndex++,
							accountDao);
					dbTransfer.setBlock(dbBlock);
					importanceTransferTransactions.add(dbTransfer);
				}
				break;
			}
		}

		dbBlock.setBlockTransfers(transferTransactions);
		dbBlock.setBlockImportanceTransfers(importanceTransferTransactions);
		return dbBlock;
	}

	/**
	 * Converts a Block db-model to a Block model.
	 *
	 * @param dbBlock The block db-model.
	 * @param accountLookup The account lookup object.
	 * @return The Block model.
	 */
	public static Block toModel(final org.nem.nis.dbmodel.Block dbBlock, final AccountLookup accountLookup) {
		if (1 == dbBlock.getHeight()) {
			return NemesisBlock.fromResource(new DeserializationContext(accountLookup));
		}

		final Address foragerAddress = Address.fromDbAccount(dbBlock.getForger());
		final Account forager = accountLookup.findByAddress(foragerAddress);
		final Address lessorAddress = dbBlock.getLessor() != null ? Address.fromDbAccount(dbBlock.getLessor()) : null;
		final Account lessor = lessorAddress != null ? accountLookup.findByAddress(lessorAddress) : null;

		final Block block = new org.nem.core.model.Block(
				forager,
				dbBlock.getPrevBlockHash(),
				dbBlock.getGenerationHash(),
				new TimeInstant(dbBlock.getTimeStamp()),
				new BlockHeight(dbBlock.getHeight()));

		final Long difficulty = dbBlock.getDifficulty();
		block.setDifficulty(new BlockDifficulty(null == difficulty ? 0L : difficulty));
		block.setLessor(lessor);
		block.setSignature(new Signature(dbBlock.getForgerProof()));

		final int count = dbBlock.getBlockImportanceTransfers().size() + dbBlock.getBlockTransfers().size();
		final ArrayList<Transaction> transactions = new ArrayList<>(Arrays.asList(new Transaction[count]));

		for (final ImportanceTransfer dbTransfer : dbBlock.getBlockImportanceTransfers()) {
			final ImportanceTransferTransaction importanceTransferTransaction = ImportanceTransferMapper.toModel(dbTransfer, accountLookup);
			transactions.set(dbTransfer.getBlkIndex(), importanceTransferTransaction);
		}

		for (final Transfer dbTransfer : dbBlock.getBlockTransfers()) {
			final TransferTransaction transfer = TransferMapper.toModel(dbTransfer, accountLookup);
			transactions.set(dbTransfer.getBlkIndex(), transfer);
		}

		block.addTransactions(transactions);
		return block;
	}
}