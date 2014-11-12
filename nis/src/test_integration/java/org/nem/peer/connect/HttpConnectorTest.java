package org.nem.peer.connect;

import net.minidev.json.*;
import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.nem.core.connect.*;
import org.nem.core.crypto.*;
import org.nem.core.deploy.CommonStarter;
import org.nem.core.model.Address;
import org.nem.core.node.*;
import org.nem.core.serialization.DeserializationContext;
import org.nem.core.test.ExceptionAssert;
import org.nem.nis.*;
import org.nem.peer.Config;
import org.nem.peer.node.ImpersonatingPeerException;

import java.io.InputStream;
import java.util.*;
import java.util.concurrent.CompletionException;
import java.util.logging.Logger;

public class HttpConnectorTest {
	private static final Logger LOGGER = Logger.getLogger(HttpConnectorTest.class.getName());

	private final Communicator communicator = new HttpCommunicator(
			new HttpMethodClient<>(),
			CommunicationMode.JSON,
			new DeserializationContext(new AccountCache()));
	private final HttpConnector connector = new HttpConnector(this.communicator);

	@Test
	public void canCommunicateWithChainNemNinjaWithExpectedBootAddress() {
		// Arrange:
		final PublicKey publicKey = PublicKey.fromHexString("b13e6fde0178dd7d32017ee4f932b329d8bcaf3f2cf186b3bb10a43182c417b1");
		final Node node = createChainNemNinjaNode(publicKey);

		// Act:
		final Node remoteNode = this.connector.getInfo(node).join();

		// Assert:
		Assert.assertThat(remoteNode.getIdentity().getKeyPair().getPublicKey(), IsEqual.equalTo(publicKey));
	}

	@Test
	public void cannotCommunicateWithChainNemNinjaWithUnexpectedBootAddress() {
		// Arrange:
		final PublicKey publicKey = PublicKey.fromHexString("494e58ec8855c7a6087411506cbadbce35ce0ee76ba0baf2305c2196606fac41");
		final Node node = createChainNemNinjaNode(publicKey);

		// Act:
		ExceptionAssert.assertThrowsCompletionException(
				v -> this.connector.getInfo(node).join(),
				ImpersonatingPeerException.class);
	}

	private static Node createChainNemNinjaNode(final PublicKey publicKey) {
		final Address address = Address.fromPublicKey(publicKey);
		return new Node(
				new NodeIdentity(new KeyPair(address.getPublicKey())),
				NodeEndpoint.fromHost("chain.nem.ninja"));
	}

	@Test
	public void analyzePreTrustedPeers() {
		// Arrange:
		final Config config = new Config(
				new Node(new NodeIdentity(new KeyPair()), NodeEndpoint.fromHost("localhost")),
				loadJsonObject("peers-config.json"),
				CommonStarter.META_DATA.getVersion());

		// Act:
		final boolean result = this.analyzeNodes(config.getPreTrustedNodes().getNodes());

		// Assert:
		Assert.assertThat(result, IsEqual.equalTo(true));
	}

	private enum NodeStatus {
		ONLINE,
		IMPERSONATING,
		TIMED_OUT,
		FAILED
	}

	private boolean analyzeNodes(final Collection<Node> nodes) {
		final Map<NodeStatus, Integer> nodeStatusCounts = new HashMap<>();

		for (final Node node : nodes) {
			final NodeStatus status = this.analyzeNode(node);
			nodeStatusCounts.put(status, nodeStatusCounts.getOrDefault(status, 0) + 1);
		}

		final StringBuilder builder = new StringBuilder();
		builder.append(String.format("%s nodes", nodes.size()));
		for (final Map.Entry<NodeStatus, Integer> pair : nodeStatusCounts.entrySet()) {
			builder.append(System.lineSeparator());
			builder.append(String.format("%s nodes -> %s", pair.getValue(), pair.getKey()));
		}

		LOGGER.info(builder.toString());
		return 0 == nodeStatusCounts.getOrDefault(NodeStatus.IMPERSONATING, 0);
	}

	private NodeStatus analyzeNode(final Node node) {
		try {
			this.connector.getInfo(node).join();
			LOGGER.info(String.format("%s is configured correctly!", node));
			return NodeStatus.ONLINE;
		} catch (final CompletionException e) {
			final Throwable innerException = e.getCause();
			LOGGER.warning(String.format("%s is misbehaving: %s", node, innerException));
			if (ImpersonatingPeerException.class.isAssignableFrom(innerException.getClass())) {
				return NodeStatus.IMPERSONATING;
			} else if (InactivePeerException.class.isAssignableFrom(innerException.getClass())) {
				return NodeStatus.TIMED_OUT;
			} else {
				return NodeStatus.FAILED;
			}
		}
	}

	private static JSONObject loadJsonObject(final String configFileName) {
		try {
			try (final InputStream fin = NisPeerNetworkHost.class.getClassLoader().getResourceAsStream(configFileName)) {
				if (null == fin) {
					throw new FatalConfigException(String.format("Configuration file <%s> not available", configFileName));
				}

				return (JSONObject)JSONValue.parse(fin);
			}
		} catch (final Exception e) {
			throw new FatalConfigException("Exception encountered while loading config", e);
		}
	}
}