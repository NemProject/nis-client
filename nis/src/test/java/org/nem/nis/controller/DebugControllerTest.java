package org.nem.nis.controller;

import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.async.NemAsyncTimerVisitor;
import org.nem.core.model.*;
import org.nem.core.model.primitive.*;
import org.nem.core.serialization.SerializableList;
import org.nem.core.test.*;
import org.nem.core.time.TimeInstant;
import org.nem.nis.*;
import org.nem.nis.audit.AuditCollection;
import org.nem.nis.boot.NisPeerNetworkHost;
import org.nem.nis.cache.NisCache;
import org.nem.nis.controller.viewmodels.BlockDebugInfo;
import org.nem.nis.dao.BlockDao;
import org.nem.nis.dbmodel.*;
import org.nem.nis.poi.ImportanceCalculator;
import org.nem.nis.state.AccountState;
import org.nem.peer.PeerNetwork;

import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

public class DebugControllerTest {

	@Test
	public void timersInfoDelegatesToHost() {
		// Arrange:
		final List<NemAsyncTimerVisitor> originalVisitors = Arrays.asList(
				new NemAsyncTimerVisitor("foo", null),
				new NemAsyncTimerVisitor("bar", null));
		final TestContext context = new TestContext();
		Mockito.when(context.host.getVisitors()).thenReturn(originalVisitors);

		// Act:
		final SerializableList<NemAsyncTimerVisitor> visitors = context.controller.timersInfo();

		// Assert:
		Mockito.verify(context.host, Mockito.times(1)).getVisitors();
		Assert.assertThat(
				visitors.asCollection().stream().map(NemAsyncTimerVisitor::getTimerName).collect(Collectors.toList()),
				IsEquivalent.equivalentTo("foo", "bar"));
	}

	@Test
	public void incomingConnectionsInfoDelegatesToHost() {
		// Arrange:
		final TestContext context = new TestContext();
		final AuditCollection hostAuditCollection = Mockito.mock(AuditCollection.class);
		Mockito.when(context.host.getIncomingAudits()).thenReturn(hostAuditCollection);

		// Act:
		final AuditCollection auditCollection = context.controller.incomingConnectionsInfo();

		// Assert:
		Assert.assertThat(auditCollection, IsSame.sameInstance(hostAuditCollection));
		Mockito.verify(context.host, Mockito.times(1)).getIncomingAudits();
	}

	@Test
	public void outgoingConnectionsInfoDelegatesToHost() {
		// Arrange:
		final TestContext context = new TestContext();
		final AuditCollection hostAuditCollection = Mockito.mock(AuditCollection.class);
		Mockito.when(context.host.getOutgoingAudits()).thenReturn(hostAuditCollection);

		// Act:
		final AuditCollection auditCollection = context.controller.outgoingConnectionsInfo();

		// Assert:
		Assert.assertThat(auditCollection, IsSame.sameInstance(hostAuditCollection));
		Mockito.verify(context.host, Mockito.times(1)).getOutgoingAudits();
	}

	private static class TestContext {
		private final PeerNetwork network;
		private final NisPeerNetworkHost host;
		private final DebugController controller;

		private TestContext() {
			this.network = Mockito.mock(PeerNetwork.class);
			Mockito.when(this.network.getLocalNode()).thenReturn(NodeUtils.createNodeWithName("l"));

			this.host = Mockito.mock(NisPeerNetworkHost.class);
			Mockito.when(this.host.getNetwork()).thenReturn(this.network);

			this.controller = new DebugController(this.host);
		}
	}
}