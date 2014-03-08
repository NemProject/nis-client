package org.nem.peer.v2;

import org.nem.core.serialization.*;

import java.util.*;

/**
 * Represents a collection of nodes.
 * TODO: rename to NodeCollection
 */
public class NodeStatusDemux implements SerializableEntity {

    private static ObjectDeserializer<Node> NODE_DESERIALIZER = new ObjectDeserializer<Node>() {
        @Override
        public Node deserialize(final Deserializer deserializer) {
            return new Node(deserializer);
        }
    };

    final Set<Node> activeNodes;
    final Set<Node> inactiveNodes;

    /**
     * Creates a node collection.
     */
    public NodeStatusDemux() {
        this.activeNodes = new HashSet<>();
        this.inactiveNodes = new HashSet<>();
    }

    /**
     * Deserializes a node collection.
     *
     * @param deserializer The deserializer.
     */
    public NodeStatusDemux(final Deserializer deserializer) {
        this.activeNodes = new HashSet<>(deserializer.readObjectArray("active", NODE_DESERIALIZER));
        this.inactiveNodes = new HashSet<>(deserializer.readObjectArray("inactive", NODE_DESERIALIZER));
    }

    /**
     * Gets a collection of active nodes.
     *
     * @return A collection of active nodes.
     */
    public Collection<Node> getActiveNodes() { return this.activeNodes; }

    /**
     * Gets a collection of inactive nodes.
     *
     * @return A collection of active nodes.
     */
    public Collection<Node> getInactiveNodes() { return this.inactiveNodes; }

    /**
     * Updates this collection to include the specified node with the associated status.
     * The new node information will replace any previous node information.
     *
     * @param node The node.
     * @param status The node status.
     */
    public void update(final Node node, final NodeStatus status) {
        if (null == node)
            throw new NullPointerException("node cannot be null");

        this.activeNodes.remove(node);
        this.inactiveNodes.remove(node);

        final Set<Node> nodes;
        switch (status) {
            case ACTIVE:
                nodes = this.activeNodes;
                break;

            case INACTIVE:
                nodes = this.inactiveNodes;
                break;

            case FAILURE:
            default:
                return;
        }

        nodes.add(node);
    }

    @Override
    public void serialize(final Serializer serializer) {
        serializer.writeObjectArray("active", new ArrayList<>(this.activeNodes));
        serializer.writeObjectArray("inactive", new ArrayList<>(this.inactiveNodes));
    }
}
