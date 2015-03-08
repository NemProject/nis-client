package org.nem.nis;

import net.minidev.json.*;
import org.nem.core.async.NemAsyncTimerVisitor;
import org.nem.core.deploy.CommonStarter;
import org.nem.core.node.*;
import org.nem.deploy.*;
import org.nem.nis.audit.AuditCollection;
import org.nem.nis.boot.*;
import org.nem.nis.cache.ReadOnlyNisCache;
import org.nem.nis.service.ChainServices;
import org.nem.nis.time.synchronization.*;
import org.nem.nis.time.synchronization.filter.*;
import org.nem.peer.*;
import org.nem.peer.connect.*;
import org.nem.peer.services.PeerNetworkServicesFactory;
import org.nem.peer.trust.score.NodeExperiences;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.InputStream;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

/**
 * NIS PeerNetworkHost
 */
public class NisPeerNetworkHost implements AutoCloseable {
	private final ReadOnlyNisCache nisCache;
	private final CountingBlockSynchronizer synchronizer;
	private final PeerNetworkScheduler scheduler;
	private final ChainServices chainServices;
	private final NisConfiguration nisConfiguration;
	private final HttpConnectorPool httpConnectorPool;
	private final AuditCollection incomingAudits;
	private final AuditCollection outgoingAudits;
	private final AtomicReference<PeerNetworkBootstrapper> peerNetworkBootstrapper = new AtomicReference<>();
	private PeerNetwork network;

	/**
	 * Creates a NIS peer network host.
	 *
	 * @param nisCache The nis cache.
	 * @param synchronizer The block synchronizer.
	 * @param scheduler The network scheduler.
	 * @param chainServices The remote block chain service.
	 * @param nisConfiguration The nis configuration.
	 * @param httpConnectorPool The factory of http connectors.
	 * @param incomingAudits The incoming audits
	 * @param outgoingAudits The outgoing audits.
	 */
	@Autowired(required = true)
	public NisPeerNetworkHost(
			final ReadOnlyNisCache nisCache,
			final CountingBlockSynchronizer synchronizer,
			final PeerNetworkScheduler scheduler,
			final ChainServices chainServices,
			final NisConfiguration nisConfiguration,
			final HttpConnectorPool httpConnectorPool,
			final AuditCollection incomingAudits,
			final AuditCollection outgoingAudits) {
		this.nisCache = nisCache;
		this.synchronizer = synchronizer;
		this.scheduler = scheduler;
		this.chainServices = chainServices;
		this.nisConfiguration = nisConfiguration;
		this.httpConnectorPool = httpConnectorPool;
		this.incomingAudits = incomingAudits;
		this.outgoingAudits = outgoingAudits;
	}

	/**
	 * Boots the network.
	 *
	 * @param localNode The local node.
	 * @return Void future.
	 */
	public CompletableFuture boot(final Node localNode) {
		final Config config = new Config(
				localNode,
				loadJsonObject("peers-config.json"),
				CommonStarter.META_DATA.getVersion(),
				this.nisConfiguration.getOptionalFeatures());

		this.peerNetworkBootstrapper.compareAndSet(null, this.createPeerNetworkBootstrapper(config));
		return this.peerNetworkBootstrapper.get().boot().thenAccept(network -> {
			this.network = network;
			this.scheduler.addTasks(
					this.network,
					this.nisConfiguration.useNetworkTime(),
					IpDetectionMode.Disabled != this.nisConfiguration.getIpDetectionMode());
		});
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

	/**
	 * Gets the hosted network.
	 *
	 * @return The hosted network.
	 */
	public PeerNetwork getNetwork() {
		if (null == this.network) {
			throw new NisIllegalStateException(NisIllegalStateException.Reason.NIS_ILLEGAL_STATE_NOT_BOOTED);
		}

		return this.network;
	}

	/**
	 * Gets a value indicating whether or not the network is currently being booted.
	 *
	 * @return true if the network is being booted, false otherwise.
	 */
	public boolean isNetworkBooting() {
		return !this.isNetworkBooted() && null != this.peerNetworkBootstrapper.get();
	}

	/**
	 * Gets a value indicating whether or not the network is already booted.
	 *
	 * @return true if the network is booted, false otherwise.
	 */
	public boolean isNetworkBooted() {
		return null != this.network;
	}

	/**
	 * Gets outgoing audit information.
	 *
	 * @return The outgoing audit information.
	 */
	public AuditCollection getOutgoingAudits() {
		return this.outgoingAudits;
	}

	/**
	 * Gets incoming audit information.
	 *
	 * @return The incoming audit information.
	 */
	public AuditCollection getIncomingAudits() {
		return this.incomingAudits;
	}

	/**
	 * Gets the number of sync attempts with the specified node.
	 *
	 * @param node The node to sync with.
	 * @return The number of sync attempts with the specified node.
	 */
	public int getSyncAttempts(final Node node) {
		return this.synchronizer.getSyncAttempts(node);
	}

	/**
	 * Gets all timer visitors.
	 *
	 * @return All timer visitors.
	 */
	public List<NemAsyncTimerVisitor> getVisitors() {
		return this.scheduler.getVisitors();
	}

	@Override
	public void close() {
		this.scheduler.close();
	}

	private TimeSynchronizationStrategy createTimeSynchronizationStrategy() {
		return new DefaultTimeSynchronizationStrategy(
				new AggregateSynchronizationFilter(Arrays.asList(
						new ResponseDelayDetectionFilter(),
						new ClampingFilter(),
						new AlphaTrimmedMeanFilter())),
				this.nisCache.getPoiFacade(),
				this.nisCache.getAccountStateCache());
	}

	private PeerNetworkServicesFactory createNetworkServicesFactory(final PeerNetworkState networkState) {
		final PeerConnector peerConnector = this.httpConnectorPool.getPeerConnector(this.nisCache.getAccountCache());
		final TimeSynchronizationConnector timeSynchronizationConnector = this.httpConnectorPool.getTimeSyncConnector(this.nisCache.getAccountCache());
		return new PeerNetworkServicesFactory(
				networkState,
				peerConnector,
				timeSynchronizationConnector,
				this.httpConnectorPool,
				this.synchronizer,
				this.chainServices,
				this.createTimeSynchronizationStrategy());
	}

	private PeerNetworkBootstrapper createPeerNetworkBootstrapper(final Config config) {
		final PeerNetworkState networkState = new PeerNetworkState(config, new NodeExperiences(), new NodeCollection());
		final PeerNetworkNodeSelectorFactory selectorFactory = new PeerNetworkNodeSelectorFactory(
				this.nisConfiguration,
				config.getTrustProvider(),
				networkState,
				this.nisCache.getPoiFacade(),
				this.nisCache.getAccountStateCache());
		return new PeerNetworkBootstrapper(
				networkState,
				this.createNetworkServicesFactory(networkState),
				selectorFactory,
				this.nisConfiguration.getIpDetectionMode());
	}
}
