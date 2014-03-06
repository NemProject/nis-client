package org.nem.peer.v2;

import org.hamcrest.core.*;
import org.junit.*;
import org.nem.core.test.*;

import java.util.*;

public class NodeStatusDemuxTest {

    //region basic partitioning

    @Test
    public void activeNodeIsPlacedInActiveList() {
        // Arrange:
        assertStatusListAssignment(NodeStatus.ACTIVE, 1, 0);
    }

    @Test
    public void inactiveNodeIsPlacedInInactiveList() {
        // Arrange:
        assertStatusListAssignment(NodeStatus.INACTIVE, 0, 1);
    }

    @Test
    public void failedNodeIsNotPlacedInAnyList() {
        // Assert:
        assertStatusListAssignment(NodeStatus.FAILURE, 0, 0);
    }

     @Test
    public void inputListWithMultipleNodesIsPartitionedCorrectly() {
        // Arrange:
        final List<Node> nodes = new ArrayList<>();
        nodes.add(createNode(NodeStatus.ACTIVE, "A"));
        nodes.add(createNode(NodeStatus.INACTIVE, "B"));
        nodes.add(createNode(NodeStatus.INACTIVE, "C"));
        nodes.add(createNode(NodeStatus.ACTIVE, "D"));
        nodes.add(createNode(NodeStatus.FAILURE, "E"));
        nodes.add(createNode(NodeStatus.ACTIVE, "F"));

         // Act:
         final NodeStatusDemux demux = new NodeStatusDemux(nodes);

         // Assert:
         Assert.assertThat(
             getPlatforms(demux.getActiveNodes()),
             IsEquivalent.equivalentTo(new String[] { "A", "D", "F" }));
         Assert.assertThat(
             getPlatforms(demux.getInactiveNodes()),
             IsEquivalent.equivalentTo(new String[] { "B", "C" }));
    }

    private static List<String> getPlatforms(final Collection<NodeInfo> nodes) {
        final List<String> platforms = new ArrayList<>();
        for (final NodeInfo node : nodes)
            platforms.add(node.getPlatform());
        return platforms;
    }

    private static void assertStatusListAssignment(
        final NodeStatus status,
        final int expectedNumActiveNodes,
        final int expectedNumInactiveNodes) {
        // Arrange:
        final List<Node> nodes = createSingleItemNodeList(status, "Alpha");

        // Act:
        final NodeStatusDemux demux = new NodeStatusDemux(nodes);

        // Assert:
        Assert.assertThat(demux.getActiveNodes().size(), IsEqual.equalTo(expectedNumActiveNodes));
        Assert.assertThat(demux.getInactiveNodes().size(), IsEqual.equalTo(expectedNumInactiveNodes));
    }

    //endregion

    //region serialization

    @Test
    public void canRoundTripNodeStatusDemux() {
        // Arrange:
        final List<Node> nodes = new ArrayList<>();
        nodes.add(createNode(NodeStatus.ACTIVE, "A"));
        nodes.add(createNode(NodeStatus.INACTIVE, "B"));
        nodes.add(createNode(NodeStatus.INACTIVE, "C"));
        nodes.add(createNode(NodeStatus.ACTIVE, "D"));
        nodes.add(createNode(NodeStatus.FAILURE, "E"));
        nodes.add(createNode(NodeStatus.ACTIVE, "F"));
        final NodeStatusDemux originalDemux = new NodeStatusDemux(nodes);

        // Assert:
        NodeStatusDemux demux = new NodeStatusDemux(Utils.roundtripSerializableEntity(originalDemux, null));

        // Assert:
        Assert.assertThat(demux.getActiveNodes().size(), IsEqual.equalTo(3));
        Assert.assertThat(demux.getInactiveNodes().size(), IsEqual.equalTo(2));
    }

    //endregion

    private static List<Node> createSingleItemNodeList(final NodeStatus status, final String platform) {
        // Arrange:
        final List<Node> nodes = new ArrayList<>();
        nodes.add(createNode(status, platform));
        return nodes;
    }

    private static Node createNode(final NodeStatus status, final String platform) {
        // Arrange:
        final NodeInfo info = new NodeInfo(new NodeEndpoint("http", "localhost", 80), platform, "FooBar");
        final Node node = new Node(info);
        node.setStatus(status);
        return node;
    }
}
