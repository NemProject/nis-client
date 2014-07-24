package org.nem.peer.node;

import org.hamcrest.core.*;
import org.junit.*;
import org.mockito.Mockito;
import org.nem.core.metadata.ApplicationMetaData;
import org.nem.core.node.Node;
import org.nem.core.time.*;
import org.nem.peer.test.PeerUtils;

public class NisNodeInfoTest {

	@Test
	public void nodeInfoExposesAllConstructorParameters() {
		// Arrange:
		final Node node = PeerUtils.createNodeWithName("a");
		final ApplicationMetaData appMetaData = createAppMetaData("nem", "1.0");

		// Act:
		final NisNodeInfo nodeInfo = new NisNodeInfo(node, appMetaData);

		// Assert:
		Assert.assertThat(nodeInfo.getNode(), IsSame.sameInstance(node));
		Assert.assertThat(nodeInfo.getAppMetaData(), IsSame.sameInstance(appMetaData));
	}

	@Test
	public void canRoundtripNodeInfoMetaData() {
		// Arrange:
		final Node node = PeerUtils.createNodeWithName("b");
		final ApplicationMetaData appMetaData = createAppMetaData("nem", "1.0");

		// Act:
		final NisNodeInfo nodeInfo = roundtripNodeInfo(new NisNodeInfo(node, appMetaData));

		// Assert:
		Assert.assertThat(nodeInfo.getNode(), IsEqual.equalTo(node));
		Assert.assertThat(nodeInfo.getAppMetaData().getAppName(), IsEqual.equalTo("nem"));
	}

	private static NisNodeInfo roundtripNodeInfo(final NisNodeInfo nodeInfo) {
		return new NisNodeInfo(org.nem.core.test.Utils.roundtripSerializableEntity(nodeInfo, null));
	}

	private static ApplicationMetaData createAppMetaData(final String name, final String version) {
		final TimeProvider timeProvider = Mockito.mock(TimeProvider.class);
		Mockito.when(timeProvider.getCurrentTime()).thenReturn(new TimeInstant(17));
		return new ApplicationMetaData(name, version, null, timeProvider);
	}
}