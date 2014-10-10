package org.nem.nis.poi.graph;

import org.nem.core.model.primitive.NodeId;

/**
 * Strategy for calculating the similarity between two nodes.
 */
@FunctionalInterface
public interface SimilarityStrategy {

	/**
	 * Calculates structural similarity between two nodes.
	 *
	 * @param lhs One node id.
	 * @param rhs The other node id.
	 * @return The similarity score.
	 */
	public double calculateSimilarity(final NodeId lhs, final NodeId rhs);
}
