package org.nem.nis.poi.graph;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.model.primitive.NodeId;
import org.nem.nis.test.NisUtils;

public class DefaultSimilarityStrategyTest {

	@Test
	public void similarityScoreIsCalculatedCorrectlyWhenNodesHaveNoNeighbors() {
		// Arrange:
		final NodeNeighborMap neighborMap = Mockito.mock(NodeNeighborMap.class);
		Mockito.when(neighborMap.getNeighbors(new NodeId(2))).thenReturn(new NodeNeighbors(NisUtils.toNodeIdArray(2)));
		Mockito.when(neighborMap.getNeighbors(new NodeId(5))).thenReturn(new NodeNeighbors(NisUtils.toNodeIdArray(5)));
		final SimilarityStrategy strategy = new DefaultSimilarityStrategy(neighborMap);

		// Assert:
		Assert.assertThat(strategy.calculateSimilarity(new NodeId(5), new NodeId(2)), IsEqual.equalTo(0.0));
		Assert.assertThat(strategy.calculateSimilarity(new NodeId(2), new NodeId(5)), IsEqual.equalTo(0.0));
	}

	@Test
	public void similarityScoreIsCalculatedCorrectlyWhenNodesHaveNoCommonNeighbors() {
		// Arrange:
		final NodeNeighborMap neighborMap = Mockito.mock(NodeNeighborMap.class);
		Mockito.when(neighborMap.getNeighbors(new NodeId(2))).thenReturn(new NodeNeighbors(NisUtils.toNodeIdArray(1, 2, 3)));
		Mockito.when(neighborMap.getNeighbors(new NodeId(5))).thenReturn(new NodeNeighbors(NisUtils.toNodeIdArray(4, 5, 6)));
		final SimilarityStrategy strategy = new DefaultSimilarityStrategy(neighborMap);

		// Assert:
		Assert.assertThat(strategy.calculateSimilarity(new NodeId(5), new NodeId(2)), IsEqual.equalTo(0.0));
		Assert.assertThat(strategy.calculateSimilarity(new NodeId(2), new NodeId(5)), IsEqual.equalTo(0.0));
	}

	@Test
	public void similarityScoreIsCalculatedCorrectlyWhenNodesHaveCommonNeighbors() {
		// Arrange:
		final NodeNeighborMap neighborMap = Mockito.mock(NodeNeighborMap.class);
		Mockito.when(neighborMap.getNeighbors(new NodeId(2))).thenReturn(new NodeNeighbors(NisUtils.toNodeIdArray(0, 1, 2, 3, 4)));
		Mockito.when(neighborMap.getNeighbors(new NodeId(5))).thenReturn(new NodeNeighbors(NisUtils.toNodeIdArray(3, 4, 5, 6)));
		final SimilarityStrategy strategy = new DefaultSimilarityStrategy(neighborMap);

		// Assert:
		final double expected = 2 / Math.sqrt(20);
		Assert.assertThat(strategy.calculateSimilarity(new NodeId(5), new NodeId(2)), IsEqual.equalTo(expected));
		Assert.assertThat(strategy.calculateSimilarity(new NodeId(2), new NodeId(5)), IsEqual.equalTo(expected));
	}

	@Test
	public void similarityScoreIsCalculatedCorrectlyWhenNodesHaveCommonNeighborsAndBothPivotsAreInNeighborSet() {
		// Arrange:
		final NodeNeighborMap neighborMap = Mockito.mock(NodeNeighborMap.class);
		Mockito.when(neighborMap.getNeighbors(new NodeId(2))).thenReturn(new NodeNeighbors(NisUtils.toNodeIdArray(1, 2, 3, 4, 5)));
		Mockito.when(neighborMap.getNeighbors(new NodeId(5))).thenReturn(new NodeNeighbors(NisUtils.toNodeIdArray(2, 3, 4, 5, 6)));
		final SimilarityStrategy strategy = new DefaultSimilarityStrategy(neighborMap);

		// Assert:
		final double expected = 4 / Math.sqrt(25);
		Assert.assertThat(strategy.calculateSimilarity(new NodeId(5), new NodeId(2)), IsEqual.equalTo(expected));
		Assert.assertThat(strategy.calculateSimilarity(new NodeId(2), new NodeId(5)), IsEqual.equalTo(expected));
	}
}