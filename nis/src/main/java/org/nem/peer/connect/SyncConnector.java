package org.nem.peer.connect;

import org.nem.core.crypto.HashChain;
import org.nem.core.model.*;
import org.nem.core.model.primitive.*;
import org.nem.core.node.Node;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

/**
 * Interface that is used to sync blocks and transactions across peers.
 */
public interface SyncConnector {

	// region synchronous requests

	/**
	 * Requests information about the last block in the chain from the specified node.
	 *
	 * @param node The remote node.
	 * @return The last block.
	 */
	public Block getLastBlock(final Node node);

	/**
	 * Requests information about the block at the specified height from the specified node.
	 *
	 * @param node The remote node.
	 * @param height The block height.
	 * @return The block at the specified height
	 */
	public Block getBlockAt(final Node node, final BlockHeight height);

	/**
	 * Requests information about the hashes of all blocks in the chain after the specified height
	 * from the specified node.
	 *
	 * @param node The remote node.
	 * @param height The block height
	 * @return The hashes of all blocks in the chain after the specified height.
	 */
	public HashChain getHashesFrom(final Node node, final BlockHeight height);

	/**
	 * Requests information about all blocks in the chain after the specified height
	 * from the specified node.
	 *
	 * @param node The remote node.
	 * @param height The block height.
	 * @return All blocks in the chain after the specified height.
	 */
	public Collection<Block> getChainAfter(final Node node, final BlockHeight height);

	/**
	 * Requests information about the cumulative score of the remote chain
	 * from the specified node.
	 *
	 * @param node The remote node.
	 * @return The cumulative score for the endpoint's chain.
	 */
	public BlockChainScore getChainScore(final Node node);

	/**
	 * Requests information about the unconfirmed transaction from the specified node.
	 *
	 * @param node The remote node.
	 * @return All non conflicting unconfirmed transactions the endpoint has.
	 */
	public Collection<Transaction> getUnconfirmedTransactions(final Node node);

	// endregion

	// region asynchronous requests

	/**
	 * Requests information about the cumulative score of the remote chain.
	 *
	 * @param node The remote node.
	 * @return The completable future containing the cumulative score of the remote chain.
	 */
	public CompletableFuture<BlockChainScore> getChainScoreAsync(final Node node);

	/**
	 * Requests information about the block height of the remote chain.
	 *
	 * @param node The remote node.
	 * @return The completable future containing the block height of the remote chain.
	 */
	public CompletableFuture<BlockHeight> getChainHeightAsync(final Node node);

	// endregion
}
