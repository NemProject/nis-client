package org.nem.nis.controller;

import org.hamcrest.core.IsEqual;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.deploy.CommonStarter;
import org.nem.core.model.NemStatus;
import org.nem.core.model.ncc.NemRequestResult;
import org.nem.core.node.NodeCollection;
import org.nem.core.utils.ExceptionUtils;
import org.nem.nis.NisPeerNetworkHost;
import org.nem.peer.PeerNetwork;

public class LocalControllerTest {

	//region shutdown

	@Test
	public void shutdownDelegatesToCommonStarter() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		context.controller.shutdown();
		ExceptionUtils.propagateVoid(() -> Thread.sleep(500));

		// Assert:
		Mockito.verify(context.starter, Mockito.only()).stopServer();
	}

	//endregion

	//region heartbeat

	@Test
	public void heartbeatReturnsCorrectResult() {
		// Arrange:
		final TestContext context = new TestContext();

		// Act:
		final NemRequestResult result = context.controller.heartbeat();

		// Assert:
		Assert.assertThat(result.getCode(), IsEqual.equalTo(NemRequestResult.CODE_SUCCESS));
		Assert.assertThat(result.getType(), IsEqual.equalTo(NemRequestResult.TYPE_HEARTBEAT));
		Assert.assertThat(result.getMessage(), IsEqual.equalTo("ok"));
	}

	//endregion

	//region status

	@Test
	public void statusReturnsStatusRunningWhenNetworkIsNotBootedAndNotBooting() {
		// Arrange:
		final TestContext context = new TestContext();
		Mockito.when(context.host.isNetworkBooted()).thenReturn(false);
		Mockito.when(context.host.isNetworkBooting()).thenReturn(false);

		// Act:
		final NemRequestResult result = context.controller.status();

		// Assert:
		assertStatus(result, NemStatus.RUNNING);
	}

	@Test
	public void statusReturnsStatusBootingWhenNetworkIsNotBootedButBooting() {
		// Arrange:
		final TestContext context = new TestContext();
		Mockito.when(context.host.isNetworkBooted()).thenReturn(false);
		Mockito.when(context.host.isNetworkBooting()).thenReturn(true);

		// Act:
		final NemRequestResult result = context.controller.status();

		// Assert:
		assertStatus(result, NemStatus.BOOTING);
	}

	@Test
	public void statusReturnsStatusBootedWhenNetworkIsBootedButNotSynchronized() {
		// Arrange:
		final TestContext context = new TestContext();
		Mockito.when(context.host.isNetworkBooted()).thenReturn(true);
		Mockito.when(context.network.isChainSynchronized()).thenReturn(false);

		// Act:
		final NemRequestResult result = context.controller.status();

		// Assert:
		assertStatus(result, NemStatus.BOOTED);
	}

	@Test
	public void statusReturnsStatusSynchronizedWhenNetworkIsBootedAndSynchronized() {
		// Arrange:
		final TestContext context = new TestContext();
		Mockito.when(context.host.isNetworkBooted()).thenReturn(true);
		Mockito.when(context.network.isChainSynchronized()).thenReturn(true);

		// Act:
		final NemRequestResult result = context.controller.status();

		// Assert:
		assertStatus(result, NemStatus.SYNCHRONIZED);
	}

	@Test
	public void statusReturnsStatusNoRemoteNisWhenNetworkIsBootedAndNoRemoteNisIsAvailable() {
		// Arrange:
		final TestContext context = new TestContext();
		Mockito.when(context.host.isNetworkBooted()).thenReturn(true);
		Mockito.when(context.network.getNodes()).thenReturn(new NodeCollection());

		// Act:
		final NemRequestResult result = context.controller.status();

		// Assert:
		assertStatus(result, NemStatus.NO_REMOTE_NIS_AVAILABLE);
	}

	private static void assertStatus(final NemRequestResult result, final NemStatus expectedStatus) {
		// Assert:
		Assert.assertThat(result.getType(), IsEqual.equalTo(NemRequestResult.TYPE_STATUS));
		Assert.assertThat(result.getCode(), IsEqual.equalTo(expectedStatus.getValue()));
		Assert.assertThat(result.getMessage(), IsEqual.equalTo("status"));
	}

	//endregion

	private class TestContext {
		private final NisPeerNetworkHost host = Mockito.mock(NisPeerNetworkHost.class);
		private final PeerNetwork network = Mockito.mock(PeerNetwork.class);
		private final CommonStarter starter = Mockito.mock(CommonStarter.class);
		private final LocalController controller;

		private TestContext() {
			this.controller = new LocalController(this.host, this.starter);
			Mockito.when(this.host.getNetwork()).thenReturn(this.network);
		}
	}
}
