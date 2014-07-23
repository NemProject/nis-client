package org.nem.peer.trust;

import org.hamcrest.core.IsSame;
import org.junit.*;
import org.mockito.*;
import org.nem.core.test.IsEquivalent;
import org.nem.peer.node.*;
import org.nem.peer.test.PeerUtils;

import java.security.SecureRandom;
import java.util.*;

public class PreTrustAwareNodeSelectorTest {

	//region selectNode

	@Test
	public void selectNodeDelegatesToWrappedSelector() {
		// Arrange:
		final TestContext context = new TestContext();
		final Node node = PeerUtils.createNodeWithName("a");
		Mockito.when(context.innerSelector.selectNode()).thenReturn(node);

		// Act:
		final Node selectedNode = context.selector.selectNode();

		// Assert:
		Mockito.verify(context.innerSelector, Mockito.times(1)).selectNode();
		Assert.assertThat(selectedNode, IsSame.sameInstance(node));
	}

	//endregion

	//region selectNodes

	@Test
	public void selectNodesAddsAllPreTrustedNodesWhenAllAreOffline() {
		// Arrange:
		final TestContext context = new TestContext(PeerUtils.createNodesWithNames("p", "q", "r"));
		final List<Node> nodes = PeerUtils.createNodesWithNames("a", "p", "c");
		Mockito.when(context.innerSelector.selectNodes()).thenReturn(nodes);

		// Act:
		final List<Node> selectedNodes = context.selector.selectNodes();

		// Assert:
		Mockito.verify(context.innerSelector, Mockito.times(1)).selectNodes();
		Assert.assertThat(
				selectedNodes,
				IsEquivalent.equivalentTo(PeerUtils.createNodesWithNames("p", "q", "r", "a", "c")));
	}

	@Test
	public void selectNodesForLocalPreTrustedNodeAddsAllOtherOnlinePreTrustedNodes() {
		// Arrange:
		final TestContext context = new TestContext(
				PeerUtils.createNodesWithNames("p-a", "q-a", "r-f", "s-a", "t-i", "l"));
		context.setTestNodeStatuses();
		Mockito.when(context.innerSelector.selectNodes())
				.thenReturn(PeerUtils.createNodesWithNames("a", "p-a", "c"));

		// Act:
		final List<Node> selectedNodes = context.selector.selectNodes();

		// Assert:
		Mockito.verify(context.innerSelector, Mockito.times(1)).selectNodes();
		Assert.assertThat(
				selectedNodes,
				IsEquivalent.equivalentTo(PeerUtils.createNodesWithNames("a", "c", "p-a", "q-a", "s-a")));
	}

	@Test
	public void selectNodesForLocalNonPreTrustedNodeAddsRandomPreTrustedNodes() {
		// Arrange:
		final TestContext context = createContextForLocalNonPreTrustedNodeSelectNodes();
		Mockito.when(context.innerSelector.selectNodes())
				.thenReturn(PeerUtils.createNodesWithNames("a", "p-a", "c"));

		// Act:
		final List<Node> selectedNodes = context.selector.selectNodes();

		// Assert:
		Mockito.verify(context.innerSelector, Mockito.times(1)).selectNodes();
		Assert.assertThat(
				selectedNodes,
				IsEquivalent.equivalentTo(PeerUtils.createNodesWithNames("a", "c", "p-a", "q-a")));
	}

	@Test
	public void selectNodesForLocalNonPreTrustedNodeDoesNotAddRandomPreTrustedNodeIfNodeIsAlreadySelected() {
		// Arrange:
		final TestContext context = createContextForLocalNonPreTrustedNodeSelectNodes();
		Mockito.when(context.innerSelector.selectNodes())
				.thenReturn(PeerUtils.createNodesWithNames("a", "q-a", "c"));

		// Act:
		final List<Node> selectedNodes = context.selector.selectNodes();

		// Assert:
		Mockito.verify(context.innerSelector, Mockito.times(1)).selectNodes();
		Assert.assertThat(
				selectedNodes,
				IsEquivalent.equivalentTo(PeerUtils.createNodesWithNames("a", "c", "q-a")));
	}

	private static TestContext createContextForLocalNonPreTrustedNodeSelectNodes() {
		final Random random = Mockito.mock(Random.class);
		Mockito.when(random.nextDouble()).thenReturn(0.5);
		final TestContext context = new TestContext(
				PeerUtils.createNodesWithNames("p-a", "q-a", "r-f", "s-a", "t-i"),
				random);
		context.setTestNodeStatuses();
		return context;
	}

	//endregion

	private static class TestContext {
		private final NodeSelector innerSelector = Mockito.mock(NodeSelector.class);
		private final TrustContext context = Mockito.mock(TrustContext.class);

		private final Node localNode = PeerUtils.createNodeWithName("l");
		private final NodeCollection nodes = new NodeCollection();
		private final NodeSelector selector;

		public TestContext() {
			this(PeerUtils.createNodesWithNames("p", "q", "r"));
		}

		public TestContext(final List<Node> preTrustedNodes) {
			this(preTrustedNodes, new SecureRandom());
		}

		public TestContext(final List<Node> preTrustedNodes, final Random random) {
			Mockito.when(context.getLocalNode()).thenReturn(this.localNode);
			Mockito.when(context.getPreTrustedNodes())
					.thenReturn(new PreTrustedNodes(new LinkedHashSet<>(preTrustedNodes)));

			this.selector = new PreTrustAwareNodeSelector(this.innerSelector, this.nodes, this.context, random);
		}

		public void setNodeStatus(final String name, final NodeStatus status) {
			this.nodes.update(PeerUtils.createNodeWithName(name), status);
		}

		public void setTestNodeStatuses() {
			this.setNodeStatus("p-a", NodeStatus.ACTIVE);
			this.setNodeStatus("q-a", NodeStatus.ACTIVE);
			this.setNodeStatus("r-f", NodeStatus.FAILURE);
			this.setNodeStatus("s-a", NodeStatus.ACTIVE);
			this.setNodeStatus("t-i", NodeStatus.INACTIVE);
			this.setNodeStatus("l", NodeStatus.ACTIVE);
		}
	}
}