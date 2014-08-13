package org.nem.peer.trust;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.math.ColumnVector;
import org.nem.core.node.*;
import org.nem.peer.test.*;

public class ActiveNodeTrustProviderTest {

	@Test
	public void activeNodesAreNotFilteredOut() {
		// Act:
		final ColumnVector resultVector = getFilteredTrustVectorWithStatusAtThirdNode(NodeStatus.ACTIVE);

		// Assert: (4 nodes are active with equally-distributed initial trust)
		Assert.assertThat(resultVector.getAt(2), IsEqual.equalTo(0.25));
	}

	@Test
	public void inactiveNodesAreFilteredOut() {
		// Act:
		final ColumnVector resultVector = getFilteredTrustVectorWithStatusAtThirdNode(NodeStatus.INACTIVE);

		// Assert:
		Assert.assertThat(resultVector.getAt(2), IsEqual.equalTo(0.0));
	}

	@Test
	public void failureNodesAreFilteredOut() {
		// Act:
		final ColumnVector resultVector = getFilteredTrustVectorWithStatusAtThirdNode(NodeStatus.FAILURE);

		// Assert:
		Assert.assertThat(resultVector.getAt(2), IsEqual.equalTo(0.0));
	}

	@Test
	public void localNodeIsFilteredOut() {
		// Act:
		final ColumnVector resultVector = getFilteredTrustVectorWithStatusAtThirdNode(NodeStatus.ACTIVE);

		// Assert:
		Assert.assertThat(resultVector.getAt(resultVector.size() - 1), IsEqual.equalTo(0.0));
	}

	@Test
	public void zeroVectorIsReturnedWhenAllNodesAreInactive() {
		// Arrange:
		final TrustContext context = new TestTrustContext().getContext();
		final ColumnVector inputVector = new ColumnVector(1, 1, 1, 1, 1);

		final NodeCollection nodeCollection = createNodeCollection(context.getNodes(), NodeStatus.INACTIVE);

		final TrustProvider provider = new ActiveNodeTrustProvider(new MockTrustProvider(inputVector), nodeCollection);

		// Act:
		final ColumnVector resultVector = provider.computeTrust(context);

		// Assert:
		Assert.assertThat(resultVector, IsEqual.equalTo(new ColumnVector(5)));
	}

	@Test
	public void untrustedActiveNodesAreNotAutoTrustedIfAtLeastOneActiveNodeHasAnyTrust() {
		// Arrange:
		final TrustContext context = new TestTrustContext().getContext();
		final ColumnVector inputVector = new ColumnVector(0, 0.0001, 0, 0, 1);

		final Node[] nodes = context.getNodes();
		final NodeCollection nodeCollection = createNodeCollection(nodes, NodeStatus.ACTIVE);

		final TrustProvider provider = new ActiveNodeTrustProvider(new MockTrustProvider(inputVector), nodeCollection);

		// Act:
		final ColumnVector resultVector = provider.computeTrust(context);

		// Assert:
		Assert.assertThat(resultVector, IsEqual.equalTo(new ColumnVector(0, 1, 0, 0, 0)));
	}

	@Test
	public void untrustedActiveNodesAreAutoTrustedIfNoActiveNodesHaveAnyTrust() {
		// Arrange:
		final TrustContext context = new TestTrustContext().getContext();
		final ColumnVector inputVector = new ColumnVector(0, 0.0001, 0, 0, 1);

		final Node[] nodes = context.getNodes();
		final NodeCollection nodeCollection = createNodeCollection(nodes, NodeStatus.ACTIVE);
		nodeCollection.update(nodes[1], NodeStatus.INACTIVE);

		final TrustProvider provider = new ActiveNodeTrustProvider(new MockTrustProvider(inputVector), nodeCollection);

		// Act:
		final ColumnVector resultVector = provider.computeTrust(context);

		// Assert:
		Assert.assertThat(resultVector, IsEqual.equalTo(new ColumnVector(1.0 / 3, 0, 1.0 / 3, 1.0 / 3, 0)));
	}

	@Test
	public void trustIsDistributedProportionallyAmongActiveNodes() {
		// Arrange:
		final TrustContext context = new TestTrustContext().getContext();
		final ColumnVector inputVector = new ColumnVector(3, 1, 5, 2, 1);

		final Node[] nodes = context.getNodes();
		final NodeCollection nodeCollection = createNodeCollection(nodes, NodeStatus.ACTIVE);
		nodeCollection.update(nodes[1], NodeStatus.INACTIVE);

		final TrustProvider provider = new ActiveNodeTrustProvider(new MockTrustProvider(inputVector), nodeCollection);

		// Act:
		final ColumnVector resultVector = provider.computeTrust(context);

		// Assert:
		Assert.assertThat(resultVector, IsEqual.equalTo(new ColumnVector(0.3, 0, 0.5, 0.2, 0)));
	}

	private static ColumnVector getFilteredTrustVectorWithStatusAtThirdNode(final NodeStatus status) {
		// Arrange:
		final TrustContext context = new TestTrustContext().getContext();
		final ColumnVector inputVector = new ColumnVector(1, 1, 1, 1, 1);

		final NodeCollection nodeCollection = createNodeCollection(context.getNodes(), NodeStatus.ACTIVE);
		nodeCollection.update(context.getNodes()[2], status);

		final TrustProvider provider = new ActiveNodeTrustProvider(new MockTrustProvider(inputVector), nodeCollection);

		// Act:
		return provider.computeTrust(context);
	}

	private static NodeCollection createNodeCollection(final Node[] nodes, final NodeStatus status) {
		final NodeCollection nodeCollection = new NodeCollection();
		for (final Node node : nodes) {
			nodeCollection.update(node, status);
		}

		return nodeCollection;
	}
}
