package org.nem.peer.connect;

import org.nem.core.node.*;
import org.nem.core.serialization.*;

import java.util.concurrent.CompletableFuture;

/**
 * A interface that is used to request information from nodes.
 */
public interface PeerConnector {

	/**
	 * Gets information about the specified node.
	 *
	 * @param node The remote node.
	 * @return Information about the specified node.
	 */
	public CompletableFuture<Node> getInfo(final Node node);

	/**
	 * Requests information about all known active peers from the specified node.
	 *
	 * @param node The remote node.
	 * @return A collection of all known active peers.
	 */
	public CompletableFuture<SerializableList<Node>> getKnownPeers(final Node node);

	/**
	 * Requests information about the local (requesting) node.
	 * Can be used to determine remote IP address and refresh local node information.
	 * This enables a kind of auto-magical configuration.
	 *
	 * @param node The remote node.
	 * @param localEndpoint The local endpoint (what the local node knows about itself).
	 * @return Information about the requesting node.
	 */
	public CompletableFuture<NodeEndpoint> getLocalNodeInfo(
			final Node node,
			final NodeEndpoint localEndpoint);

	/**
	 * Announces a new entity to the target node.
	 *
	 * @param node The remote node.
	 * @param announceId The type of announcement.
	 * @param entity The entity to announce.
	 */
	public CompletableFuture announce(
			final Node node,
			final NodeApiId announceId,
			final SerializableEntity entity);
}