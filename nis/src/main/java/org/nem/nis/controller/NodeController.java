package org.nem.nis.controller;

import org.nem.core.deploy.CommonStarter;
import org.nem.core.model.primitive.BlockHeight;
import org.nem.core.node.*;
import org.nem.core.serialization.*;
import org.nem.nis.boot.*;
import org.nem.nis.controller.annotations.*;
import org.nem.nis.controller.viewmodels.ExtendedNodeExperiencePair;
import org.nem.nis.service.ChainServices;
import org.nem.peer.*;
import org.nem.peer.node.*;
import org.nem.peer.trust.score.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.stream.Collectors;

/**
 * REST node controller.
 */
@RestController
public class NodeController {
	private final NisPeerNetworkHost host;
	private final NetworkHostBootstrapper hostBootstrapper;
	private final ChainServices chainServices;
	private final NodeCompatibilityChecker compatibilityChecker;

	@Autowired(required = true)
	NodeController(
			final NisPeerNetworkHost host,
			final NetworkHostBootstrapper hostBootstrapper,
			final ChainServices chainServices,
			final NodeCompatibilityChecker compatibilityChecker) {
		this.host = host;
		this.hostBootstrapper = hostBootstrapper;
		this.chainServices = chainServices;
		this.compatibilityChecker = compatibilityChecker;
	}

	//region getInfo / getExtendedInfo

	/**
	 * Gets information about the running node.
	 *
	 * @return Information about the running node.
	 */
	@RequestMapping(value = "/node/info", method = RequestMethod.GET)
	@ClientApi
	public Node getInfo() {
		return this.host.getNetwork().getLocalNode();
	}

	/**
	 * Gets information about the running node.
	 *
	 * @param challenge The challenge.
	 * @return Information about the running node.
	 */
	@RequestMapping(value = "/node/info", method = RequestMethod.POST)
	@P2PApi
	@AuthenticatedApi
	public AuthenticatedResponse<Node> getInfo(@RequestBody final NodeChallenge challenge) {
		final Node localNode = this.host.getNetwork().getLocalNode();
		return new AuthenticatedResponse<>(localNode, localNode.getIdentity(), challenge);
	}

	/**
	 * Gets extended information about the running node.
	 *
	 * @return Extended information about the running node.
	 */
	@RequestMapping(value = "/node/extended-info", method = RequestMethod.GET)
	@ClientApi
	public NisNodeInfo getExtendedInfo() {
		return new NisNodeInfo(this.host.getNetwork().getLocalNode(), CommonStarter.META_DATA);
	}

	//endregion

	//region getPeerList / getActivePeerList

	/**
	 * Gets a list of the active and inactive nodes currently known by the
	 * running node.
	 *
	 * @return A list of the active and inactive nodes currently known by the
	 * running node.
	 */
	@RequestMapping(value = "/node/peer-list/all", method = RequestMethod.GET)
	@ClientApi
	public NodeCollection getPeerList() {
		return this.host.getNetwork().getNodes();
	}

	/**
	 * Gets a list of the active nodes currently known by the running node.
	 *
	 * @return A list of the active nodes currently known by the running node.
	 */
	@RequestMapping(value = "/node/peer-list/reachable", method = RequestMethod.GET)
	@ClientApi
	public SerializableList<Node> getReachablePeerList() {
		return new SerializableList<>(this.host.getNetwork().getNodes().getActiveNodes());
	}

	/**
	 * Gets a dynamic list of the active nodes to which the running node broadcasts information.
	 *
	 * @return A list of broadcast partners.
	 */
	@RequestMapping(value = "/node/peer-list/active", method = RequestMethod.GET)
	@ClientApi
	public SerializableList<Node> getActivePeerList() {
		return new SerializableList<>(this.host.getNetwork().getPartnerNodes());
	}

	/**
	 * Gets a dynamic list of the active nodes to which the running node broadcasts information.
	 *
	 * @param challenge The challenge.
	 * @return A list of broadcast partners.
	 */
	@RequestMapping(value = "/node/peer-list/active", method = RequestMethod.POST)
	@P2PApi
	@PublicApi
	@AuthenticatedApi
	public AuthenticatedResponse<SerializableList<Node>> getActivePeerList(@RequestBody final NodeChallenge challenge) {
		final Node localNode = this.host.getNetwork().getLocalNode();
		return new AuthenticatedResponse<>(this.getActivePeerList(), localNode.getIdentity(), challenge);
	}

	//endregion

	/**
	 * Gets the local node's experiences.
	 *
	 * @return Information about the experiences the local node has had with other nodes.
	 */
	@RequestMapping(value = "/node/experiences", method = RequestMethod.GET)
	@P2PApi
	@PublicApi
	public SerializableList<ExtendedNodeExperiencePair> getExperiences() {
		final NodeExperiencesPair pair = this.host.getNetwork().getLocalNodeAndExperiences();

		final List<ExtendedNodeExperiencePair> nodeExperiencePairs = new ArrayList<>(pair.getExperiences().size());
		nodeExperiencePairs.addAll(pair.getExperiences().stream().map(this::extend).collect(Collectors.toList()));
		return new SerializableList<>(nodeExperiencePairs);
	}

	private ExtendedNodeExperiencePair extend(final NodeExperiencePair pair) {
		return new ExtendedNodeExperiencePair(
				pair.getNode(),
				pair.getExperience(),
				this.host.getSyncAttempts(pair.getNode()));
	}

	/**
	 * Ping that means the pinging node is part of the NEM P2P network.
	 *
	 * @param nodeExperiencesPair Information about the experiences the pinging node has had
	 * with other nodes.
	 */
	@RequestMapping(value = "/node/ping", method = RequestMethod.POST)
	@P2PApi
	public void ping(@RequestBody final NodeExperiencesPair nodeExperiencesPair) {
		final PeerNetwork network = this.host.getNetwork();
		final Node node = nodeExperiencesPair.getNode();
		if (!this.compatibilityChecker.check(network.getLocalNode().getMetaData(), node.getMetaData())) {
			// silently ignore pings from incompatible nodes
			return;
		}

		if (NodeStatus.UNKNOWN == network.getNodes().getNodeStatus(node)) {
			network.getNodes().update(node, NodeStatus.ACTIVE);
		}

		network.setRemoteNodeExperiences(nodeExperiencesPair);
	}

	/**
	 * Just return the Node info the requester "Can You See Me" using the IP
	 * address from the request.
	 *
	 * @param localEndpoint The local endpoint (what the local node knows about itself).
	 * @param request The http servlet request.
	 * @return The endpoint.
	 */
	@RequestMapping(value = "/node/cysm", method = RequestMethod.POST)
	@P2PApi
	public NodeEndpoint canYouSeeMe(
			@RequestBody final NodeEndpoint localEndpoint,
			final HttpServletRequest request) {
		// request.getRemotePort() is never the port on which the node is listening,
		// so let the client specify its desired port
		return new NodeEndpoint(
				localEndpoint.getBaseUrl().getProtocol(),
				request.getRemoteAddr(),
				localEndpoint.getBaseUrl().getPort());
	}

	/**
	 * Boots the network.
	 *
	 * @param deserializer Information about the primary node including the private key.
	 */
	@RequestMapping(value = "/node/boot", method = RequestMethod.POST)
	@ClientApi
	@TrustedApi
	public void boot(@RequestBody final Deserializer deserializer) {
		final Node localNode = new LocalNodeDeserializer().deserialize(deserializer);
		this.hostBootstrapper.boot(localNode);
	}

	//region activePeersMaxChainHeight

	@RequestMapping(value = "/node/active-peers/max-chain-height", method = RequestMethod.GET)
	@PublicApi
	public BlockHeight activePeersMaxChainHeight() {
		return this.chainServices.getMaxChainHeightAsync(this.getActivePeerList().asCollection()).join();
	}

	//endregion
}
