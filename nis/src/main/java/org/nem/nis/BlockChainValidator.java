package org.nem.nis;

import org.nem.core.model.*;

import java.math.BigInteger;
import java.util.Collection;

/**
 * Helper class for validating a block chain.
 */
public class BlockChainValidator {

	private final int maxChainSize;
	private final BlockScorer scorer;

	/**
	 * Creates a new block chain validator.
	 *
	 * @param scorer The block scorer to use.
	 */
	public BlockChainValidator(final BlockScorer scorer, final int maxChainSize) {
		this.scorer = scorer;
		this.maxChainSize = maxChainSize;
	}

	/**
	 * Determines if blocks is a valid block chain given blocks and parentBlock.
	 *
	 * @param parentBlock The parent block.
	 * @param blocks The block chain.
	 * @return true if the blocks are valid.
	 */
	public boolean isValid(Block parentBlock, final Collection<Block> blocks) {
		if (blocks.size() > this.maxChainSize)
			return false;

		BlockHeight expectedHeight = parentBlock.getHeight().next();
		for (final Block block : blocks) {
			block.setPrevious(parentBlock);
			if (!expectedHeight.equals(block.getHeight()) || !block.verify() || !isBlockHit(parentBlock, block))
				return false;

			for (final Transaction transaction : block.getTransactions()) {
				if (!transaction.isValid() || !transaction.verify())
					return false;
			}

			parentBlock = block;
			expectedHeight = expectedHeight.next();
		}

		return true;
	}

	private boolean isBlockHit(final Block parentBlock, final Block block) {
		final BigInteger hit = this.scorer.calculateHit(block);
		final BigInteger target = this.scorer.calculateTarget(parentBlock, block);
		return hit.compareTo(target) < 0;
	}
}
