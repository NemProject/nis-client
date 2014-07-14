package org.nem.peer.services;

import org.hamcrest.core.IsNull;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.peer.*;
import org.nem.peer.connect.*;

public class PeerNetworkServicesFactoryTest {

	@Test
	public void createInactiveNodePrunerReturnsNonNull() {
		// Assert:
		Assert.assertThat(createFactory().createInactiveNodePruner(), IsNull.notNullValue());
	}

	@Test
	public void createLocalNodeEndpointUpdaterReturnsNonNull() {
		// Assert:
		Assert.assertThat(createFactory().createLocalNodeEndpointUpdater(), IsNull.notNullValue());
	}

	@Test
	public void createNodeBroadcasterReturnsNonNull() {
		// Assert:
		Assert.assertThat(createFactory().createNodeBroadcaster(), IsNull.notNullValue());
	}

	@Test
	public void createNodeRefresherReturnsNonNull() {
		// Assert:
		Assert.assertThat(createFactory().createNodeRefresher(), IsNull.notNullValue());
	}

	@Test
	public void createNodeSynchronizerReturnsNonNull() {
		// Assert:
		Assert.assertThat(createFactory().createNodeSynchronizer(), IsNull.notNullValue());
	}

	private static PeerNetworkServicesFactory createFactory() {
		return new PeerNetworkServicesFactory(
				Mockito.mock(PeerNetworkState.class),
				Mockito.mock(PeerConnector.class),
				Mockito.mock(SyncConnectorPool.class),
				Mockito.mock(BlockSynchronizer.class));
	}
}