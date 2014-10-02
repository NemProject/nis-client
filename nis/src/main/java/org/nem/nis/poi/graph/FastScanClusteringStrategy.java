package org.nem.nis.poi.graph;

import org.nem.core.model.primitive.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of Shiokawa et al., 2014, "構造的類似度に基づくグラフクラスタリングの高速化 (Fast Structural Similarity Graph Clustering),"
 * http://db-event.jpn.org/deim2014/final/proceedings/D6-2.pdf
 */
public class FastScanClusteringStrategy implements GraphClusteringStrategy {

	@Override
	public ClusteringResult cluster(final Neighborhood neighborhood) {
		final FastScan impl = new FastScan(neighborhood);
		impl.scan();
		return impl.getClusters();
	}

	/**
	 * FastScan class uses the fast scan algorithm to cluster vertices (nodes) of a (transaction) graph
	 * into different groups: regular clusters, hubs and outliers.
	 */
	private static class FastScan extends AbstractScan {

		public FastScan(final Neighborhood neighborhood) {
			super(neighborhood);
		}

		@Override
		public void cluster(final Community community) {
			NodeNeighbors visited = new NodeNeighbors();
			visited.addNeighbor(community.getPivotId());
			this.addToCluster(community);

			NodeNeighbors unvisitedTwoHopPivots = this.neighborhood.getTwoHopAwayNeighbors(community.getPivotId());
			while (0 < unvisitedTwoHopPivots.size()) {
				this.clusterNeighbors(unvisitedTwoHopPivots);
				visited = NodeNeighbors.union(visited, unvisitedTwoHopPivots);
				unvisitedTwoHopPivots = getVisitedTwoHopUnion(unvisitedTwoHopPivots, visited);
			}
		}

		private void clusterNeighbors(final NodeNeighbors neighbors) {
			for (final NodeId id : neighbors) {
				final Community community = this.neighborhood.getCommunity(id);
				this.addToCluster(community);
			}
		}

		/**
		 * Calls addToClusterWithoutSpecialVisit and handles the special case that the node is at the end of a line graph.
		 *
		 * @param community The community to add.
		 */
		public void addToCluster(final Community community) {
			this.addToClusterWithoutSpecialVisit(community);

			if (2 != community.getSimilarNeighbors().size()) {
				return;
			}

			// Handle the special case that the node is at the end of a line graph. This doesn't seem
			// to work with regular FastScanClusteringStrategy because of the two-hop skipping
			// (pretty sure it is a limitation of the published algorithm).
			// We handle this case by creating a community around the neighboring node, rather
			// than just the two-hop away node. This is done only for that one neighbor and
			// does not propagate through subsequent iterations (i.e., we don't get the two-hop
			// away nodes for this one neighbor).
			for (final NodeId neighborId : community.getSimilarNeighbors()) {
				if (community.getPivotId() == neighborId) {
					continue;
				}

				final Community neighborCommunity = this.neighborhood.getCommunity(neighborId);
				this.addToClusterWithoutSpecialVisit(neighborCommunity);
			}
		}

		/**
		 * If the community belongs to the core,
		 * then if the community overlaps an existing cluster,
		 * - then merge the community into that cluster,
		 * - else create a new cluster,
		 * else mark community as non member.
		 * <br/>
		 * <em>This function should only be called by addToCluster.</em>
		 *
		 * @param community The community to add.
		 */
		private void addToClusterWithoutSpecialVisit(final Community community) {
			if (!community.isCore()) {
				this.markAsNonCore(community);
				return;
			}

			final Cluster cluster;
			ClusterId clusterId = new ClusterId(community.getPivotId());

			// Find out if some of the similar neighbors already have an id.
			// This would mean the new cluster overlaps with at least one existing cluster.
			// In that case we merge the existing clusters and then expand the cluster.
			final List<ClusterId> clusterIds = community.getSimilarNeighbors().toList().stream()
					.filter(nodeId -> this.isClustered(nodeId.getRaw()))
					.map(nodeId -> this.nodeStates[nodeId.getRaw()])
					.distinct()
					.map(ClusterId::new)
					.collect(Collectors.toList());
			if (!clusterIds.isEmpty()) {
				cluster = mergeClusters(clusterIds);
				clusterId = cluster.getId();
			} else {
				cluster = new Cluster(clusterId);
				this.clusters.add(cluster);
			}

			for (final NodeId nodeId : community.getSimilarNeighbors()) {
				this.nodeStates[nodeId.getRaw()] = clusterId.getRaw();
				cluster.add(nodeId);
			}
		}

		/**
		 * Merge existing clusters.
		 *
		 * @param clusterIds The ids of the clusters to merge
		 * @return The merged cluster.
		 */
		private Cluster mergeClusters(final List<ClusterId> clusterIds) {
			if (0 >= clusterIds.size()) {
				throw new IllegalArgumentException("need at least one cluster id to merge");
			}

			final ClusterId clusterId = clusterIds.get(0);
			final Cluster cluster = findCluster(clusterId);

			// TODO 20141002: we should add a test were clusters get merged
			for (int ndx = 1; ndx < clusterIds.size(); ndx++) {
				final Cluster clusterToMerge = findCluster(clusterIds.get(ndx));
				cluster.merge(clusterToMerge);
				clusterToMerge.getMemberIds().stream().forEach(id -> this.nodeStates[id.getRaw()] = clusterId.getRaw());
				this.clusters.remove(clusterToMerge);
			}

			return cluster;
		}

		/**
		 * Returns a collection of all unvisited node ids that are two hops away
		 * from an already visited collection of nodes.
		 *
		 * @param newlyVisited - Nodes visited in the last iteration of the while loop.
		 * @param visitedTwoHopUnion - nodes visited up until now.
		 * @return The collection of nodes that are two hops away from the set of newlyVisited nodes.
		 */
		private NodeNeighbors getVisitedTwoHopUnion(final NodeNeighbors newlyVisited, final NodeNeighbors visitedTwoHopUnion) {
			final NodeNeighbors[] twoHopAwayNodeNeighbors = new NodeNeighbors[newlyVisited.size()];

			int index = 0;
			for (final NodeId u : newlyVisited) {
				twoHopAwayNodeNeighbors[index++] = this.neighborhood.getTwoHopAwayNeighbors(u);
			}

			NodeNeighbors unvisitedTwoHopUnion = NodeNeighbors.union(twoHopAwayNodeNeighbors);
			unvisitedTwoHopUnion = unvisitedTwoHopUnion.difference(visitedTwoHopUnion);
			return unvisitedTwoHopUnion;
		}

		/**
		 * Returns the cluster with a given id from the cluster collection.
		 *
		 * @param clusterId The id for the cluster.
		 * @return The cluster with the wanted id.
		 */
		private Cluster findCluster(final ClusterId clusterId) {
			for (final Cluster cluster : this.clusters) {
				if (cluster.getId().equals(clusterId)) {
					return cluster;
				}
			}

			throw new IllegalArgumentException("cluster with id " + clusterId + " not found.");
		}
	}
}
