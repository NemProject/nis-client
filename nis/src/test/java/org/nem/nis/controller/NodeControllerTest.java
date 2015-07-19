package org.nem.nis.controller;

import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.*;
import org.nem.core.crypto.KeyPair;
import org.nem.core.metadata.ApplicationMetaData;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.node.*;
import org.nem.core.serialization.*;
import org.nem.core.test.*;
import org.nem.deploy.CommonStarter;
import org.nem.nis.boot.*;
import org.nem.nis.controller.viewmodels.ExtendedNodeExperiencePair;
import org.nem.peer.PeerNetwork;
import org.nem.peer.node.*;
import org.nem.peer.services.ChainServices;
import org.nem.peer.trust.score.*;

import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class NodeControllerTest {

	//region getInfo / getExtendedInfo

	@Test
	public void getInfoReturnsNetworkLocalNode() {
		// Arrange:
		final TestContext context = new TestContext();

		// Assert:
		runInfoTest(context, c -> c.controller.getInfo(), n -> n);
	}

	@Test
	public void getInfoAuthenticatedReturnsNetworkLocalNode() {
		// Arrange:
		final TestContext context = new TestContext();
		final Node localNode = context.network.getLocalNode();
		final NodeChallenge challenge = new NodeChallenge(Utils.generateRandomBytes());

		// Assert:
		final AuthenticatedResponse<?> response = runInfoTest(
				context,
				c -> c.controller.getInfo(challenge),
				r -> r.getEntity(localNode.getIdentity(), challenge));
		Assert.assertThat(response.getSignature(), IsNull.notNullValue());
	}

	private static <T> T runInfoTest(
			final TestContext context,
			final Function<TestContext, T> action,
			final Function<T, Node> getNode) {
		// Act:
		final T response = action.apply(context);
		final Node node = getNode.apply(response);

		// Assert:
		Assert.assertThat(node, IsEqual.equalTo(context.network.getLocalNode()));
		return response;
	}

	@Test
	public void getExtendedInfoReturnsNetworkLocalNode() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		final Node node = context.controller.getExtendedInfo().getNode();

		// Assert:
		Assert.assertThat(node, IsSame.sameInstance(context.network.getLocalNode()));
	}

	@Test
	public void getExtendedInfoReturnsCommonStarterApplicationMetaData() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		final ApplicationMetaData appMetaData = context.controller.getExtendedInfo().getAppMetaData();

		// Assert:
		Assert.assertThat(appMetaData, IsSame.sameInstance(CommonStarter.META_DATA));
	}

	//endregion

	//region getPeerList / getReachablePeerList / getActivePeerList

	@Test
	public void getPeerListReturnsNetworkNodes() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		final NodeCollection nodes = context.controller.getPeerList();

		// Assert:
		Assert.assertThat(nodes, IsSame.sameInstance(context.network.getNodes()));
	}

	@Test
	public void getReachablePeerListReturnsActiveNetworkNodes() {
		// Arrange:
		final TestContext context = new TestContext();

		// Arrange:
		final NodeCollection nodeCollection = context.network.getNodes();
		nodeCollection.update(NodeUtils.createNodeWithHost("10.0.0.2"), NodeStatus.INACTIVE);
		nodeCollection.update(NodeUtils.createNodeWithHost("10.0.0.4"), NodeStatus.ACTIVE);
		nodeCollection.update(NodeUtils.createNodeWithHost("10.0.0.3"), NodeStatus.FAILURE);
		nodeCollection.update(NodeUtils.createNodeWithHost("10.0.0.7"), NodeStatus.ACTIVE);

		// Act:
		final SerializableList<Node> nodes = context.controller.getReachablePeerList();

		// Assert:
		final List<Node> expectedNodes = Arrays.asList(
				NodeUtils.createNodeWithHost("10.0.0.4"),
				NodeUtils.createNodeWithHost("10.0.0.7"));
		Assert.assertThat(nodes.asCollection(), IsEquivalent.equivalentTo(expectedNodes));
	}

	@Test
	public void getActivePeerListReturnsPartnerNodes() {
		// Arrange:
		final TestContext context = new TestContext();

		// Assert:
		runActivePeerListTest(context, c -> c.controller.getActivePeerList(), l -> l);
	}

	@Test
	public void getActivePeerListAuthenticatedReturnsPartnerNodes() {
		// Arrange:
		final TestContext context = new TestContext();
		final Node localNode = context.network.getLocalNode();
		final NodeChallenge challenge = new NodeChallenge(Utils.generateRandomBytes());

		// Assert:
		final AuthenticatedResponse<?> response = runActivePeerListTest(
				context,
				c -> c.controller.getActivePeerList(challenge),
				r -> r.getEntity(localNode.getIdentity(), challenge));
		Assert.assertThat(response.getSignature(), IsNull.notNullValue());
	}

	private static <T> T runActivePeerListTest(
			final TestContext context,
			final Function<TestContext, T> action,
			final Function<T, SerializableList<Node>> getActivePeerList) {
		// Arrange:
		final List<Node> selectedNodes = Arrays.asList(
				NodeUtils.createNodeWithHost("10.0.0.4"),
				NodeUtils.createNodeWithHost("10.0.0.7"));
		Mockito.when(context.network.getPartnerNodes()).thenReturn(selectedNodes);

		// Act:
		final T response = action.apply(context);
		final SerializableList<Node> nodes = getActivePeerList.apply(response);

		// Assert:
		Assert.assertThat(nodes.asCollection(), IsEquivalent.equivalentTo(selectedNodes));
		return response;
	}

	//endregion

	//region getExperiences

	@Test
	public void getExperiencesReturnsExtendedLocalNodeExperiences() {
		// Arrange:
		final TestContext context = new TestContext();
		final PeerNetwork network = context.network;
		Mockito.when(network.getLocalNodeAndExperiences()).thenReturn(
				new NodeExperiencesPair(
						NodeUtils.createNodeWithName("l"),
						Arrays.asList(
								new NodeExperiencePair(NodeUtils.createNodeWithName("n"), new NodeExperience(0, 1)),
								new NodeExperiencePair(NodeUtils.createNodeWithName("e"), new NodeExperience(1, 0)),
								new NodeExperiencePair(NodeUtils.createNodeWithName("m"), new NodeExperience(1, 0)))));

		Mockito.when(context.host.getSyncAttempts(NodeUtils.createNodeWithName("n"))).thenReturn(7);
		Mockito.when(context.host.getSyncAttempts(NodeUtils.createNodeWithName("e"))).thenReturn(0);
		Mockito.when(context.host.getSyncAttempts(NodeUtils.createNodeWithName("m"))).thenReturn(2);

		// Act:
		final Collection<ExtendedNodeExperiencePair> pairs = context.controller.getExperiences().asCollection();

		// Assert:
		final List<ExtendedNodeExperiencePair> expectedPairs = Arrays.asList(
				new ExtendedNodeExperiencePair(NodeUtils.createNodeWithName("n"), new NodeExperience(0, 1), 7),
				new ExtendedNodeExperiencePair(NodeUtils.createNodeWithName("e"), new NodeExperience(1, 0), 0),
				new ExtendedNodeExperiencePair(NodeUtils.createNodeWithName("m"), new NodeExperience(1, 0), 2));
		Assert.assertThat(pairs, IsEquivalent.equivalentTo(expectedPairs));
	}

	//endregion

	//region ping

	@Test
	public void pingActivatesSourceNodeIfSourceNodeStatusIsUnknown() {
		// Arrange:
		final TestContext context = new TestContext();
		final NodeExperiencesPair pair = new NodeExperiencesPair(NodeUtils.createNodeWithName("alice"), new ArrayList<>());

		final NodeCollection nodes = context.network.getNodes();
		nodes.update(pair.getNode(), NodeStatus.UNKNOWN);

		// Act:
		context.controller.ping(pair);

		// Assert:
		Assert.assertThat(nodes.getNodeStatus(pair.getNode()), IsEqual.equalTo(NodeStatus.ACTIVE));
	}

	@Test
	public void pingDoesNotChangeSourceNodeStatusIfSourceNodeStatusIsKnown() {
		// Arrange:
		final TestContext context = new TestContext();
		final NodeExperiencesPair pair = new NodeExperiencesPair(NodeUtils.createNodeWithName("alice"), new ArrayList<>());

		final NodeCollection nodes = context.network.getNodes();
		nodes.update(pair.getNode(), NodeStatus.INACTIVE);

		// Act:
		context.controller.ping(pair);

		// Assert:
		Assert.assertThat(nodes.getNodeStatus(pair.getNode()), IsEqual.equalTo(NodeStatus.INACTIVE));
	}

	@Test
	public void pingSetsSourceNodeExperiencesIfSourceNodeStatusIsKnown() {
		// Assert:
		assertPingSetsSourceNodeExperiencesForSourceNodeWithStatus(NodeStatus.ACTIVE);
	}

	@Test
	public void pingSetsSourceNodeExperiencesIfSourceNodeStatusIsUnknown() {
		// Assert:
		assertPingSetsSourceNodeExperiencesForSourceNodeWithStatus(NodeStatus.UNKNOWN);
	}

	private static void assertPingSetsSourceNodeExperiencesForSourceNodeWithStatus(final NodeStatus status) {
		// Arrange:
		final TestContext context = new TestContext();
		final NodeExperiencesPair pair = new NodeExperiencesPair(NodeUtils.createNodeWithName("alice"), new ArrayList<>());
		context.network.getNodes().update(pair.getNode(), status);

		// Act:
		context.controller.ping(pair);

		// Assert:
		Mockito.verify(context.network, Mockito.times(1)).setRemoteNodeExperiences(pair);
	}

	@Test
	public void pingSilentlyExitsIfRequestIsFromCrossNetworkNode() {
		// Arrange: simulate a cross-network node by returning false from context.compatibilityChecker.check
		final TestContext context = new TestContext();
		final Node remoteNode = NodeUtils.createNodeWithName("alice");
		final NodeExperiencesPair pair = new NodeExperiencesPair(remoteNode, new ArrayList<>());
		Mockito.when(context.compatibilityChecker.check(Mockito.any(), Mockito.any())).thenReturn(false);

		final NodeCollection nodes = context.network.getNodes();
		nodes.update(pair.getNode(), NodeStatus.UNKNOWN);

		// Act:
		context.controller.ping(pair);

		// Assert:
		Assert.assertThat(nodes.getNodeStatus(pair.getNode()), IsEqual.equalTo(NodeStatus.UNKNOWN));
		Mockito.verify(context.network, Mockito.never()).setRemoteNodeExperiences(pair);
		Mockito.verify(context.compatibilityChecker, Mockito.only())
				.check(context.localNode.getMetaData(), remoteNode.getMetaData());
	}

	//endregion

	@Test
	public void canYouSeeMeReturnsTheRemoteAddress() {
		// Arrange:
		final TestContext context = new TestContext();
		final NodeEndpoint localEndpoint = new NodeEndpoint("ftp", "localhost", 123);

		final HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		Mockito.when(request.getScheme()).thenReturn("https");
		Mockito.when(request.getRemoteAddr()).thenReturn("10.0.0.123");
		Mockito.when(request.getRemotePort()).thenReturn(97);

		// Act:
		final NodeEndpoint endpoint = context.controller.canYouSeeMe(localEndpoint, request);

		// Assert:
		// (1) address comes from the servlet request
		// (2) scheme and port comes from the original local node endpoint
		Assert.assertThat(endpoint, IsEqual.equalTo(new NodeEndpoint("ftp", "10.0.0.123", 123)));
	}

	@Test
	public void bootDelegatesToNetworkHostBootstrapper() {
		// Arrange:
		final TestContext context = new TestContext();
		final ArgumentCaptor<Node> nodeArgument = ArgumentCaptor.forClass(Node.class);
		Mockito.when(context.hostBootstrapper.boot(nodeArgument.capture())).thenReturn(CompletableFuture.completedFuture(null));

		final NodeIdentity identity = new NodeIdentity(new KeyPair());
		final Deserializer deserializer = createLocalNodeDeserializer(identity);

		// Act:
		context.controller.boot(deserializer);

		// Assert:
		Mockito.verify(context.hostBootstrapper, Mockito.only()).boot(Mockito.any(Node.class));
		Assert.assertThat(nodeArgument.getValue().getIdentity(), IsEqual.equalTo(identity));
	}

	@Test
	public void activePeersMaxChainHeightDelegatesToChainServices() {
		// Arrange:
		final TestContext context = new TestContext();
		final List<Node> selectedNodes = Collections.singletonList(NodeUtils.createNodeWithHost("10.0.0.4"));
		Mockito.when(context.network.getPartnerNodes()).thenReturn(selectedNodes);

		// Act:
		final BlockHeight height = context.controller.activePeersMaxChainHeight();

		// Assert:
		Mockito.verify(context.services, Mockito.only()).getMaxChainHeightAsync(selectedNodes);
		Assert.assertThat(height, IsEqual.equalTo(new BlockHeight(123)));
	}

	private static JsonDeserializer createLocalNodeDeserializer(final NodeIdentity identity) {
		// Arrange:
		final NodeEndpoint endpoint = new NodeEndpoint("http", "localhost", 8080);

		final JsonSerializer serializer = new JsonSerializer(true);
		serializer.writeObject("identity", childSerializer -> {
			childSerializer.writeBigInteger("private-key", identity.getKeyPair().getPrivateKey().getRaw());
			childSerializer.writeString("name", "trudy");
		});
		serializer.writeObject("endpoint", endpoint);
		serializer.writeObject("metaData", childSerializer -> {
			childSerializer.writeString("application", "a");
		});

		return new JsonDeserializer(serializer.getObject(), null);
	}

	private static class TestContext {
		private final PeerNetwork network = Mockito.mock(PeerNetwork.class);
		private final NisPeerNetworkHost host = Mockito.mock(NisPeerNetworkHost.class);
		private final NetworkHostBootstrapper hostBootstrapper = Mockito.mock(NetworkHostBootstrapper.class);
		private final ChainServices services = Mockito.mock(ChainServices.class);
		private final NodeCompatibilityChecker compatibilityChecker = Mockito.mock(NodeCompatibilityChecker.class);
		private final Node localNode = NodeUtils.createNodeWithName("l");
		private final NodeController controller;

		private TestContext() {
			Mockito.when(this.network.getLocalNode()).thenReturn(this.localNode);
			Mockito.when(this.network.getNodes()).thenReturn(new NodeCollection());

			Mockito.when(this.host.getNetwork()).thenReturn(this.network);

			Mockito.when(this.services.getMaxChainHeightAsync(Mockito.anyCollectionOf(Node.class)))
					.thenReturn(CompletableFuture.completedFuture(new BlockHeight(123)));

			Mockito.when(this.compatibilityChecker.check(Mockito.any(), Mockito.any())).thenReturn(true);
			this.controller = new NodeController(this.host, this.hostBootstrapper, this.services, this.compatibilityChecker);
		}
	}
}
