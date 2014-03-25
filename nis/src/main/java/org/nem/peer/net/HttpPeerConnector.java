package org.nem.peer.net;

import org.nem.core.serialization.*;
import org.nem.peer.*;

import java.net.*;

/**
 * An HTTP-based PeerConnector implementation.
 */
public class HttpPeerConnector implements PeerConnector {

    private static final int DEFAULT_TIMEOUT = 30;

    private final HttpMethodClient httpMethodClient;

    /**
     * Creates a new HTTP peer connector.
     *
     * @param context The deserialization context to use when deserializing responses.
     */
    public HttpPeerConnector(final DeserializationContext context) {
        this.httpMethodClient = new HttpMethodClient(context, DEFAULT_TIMEOUT);
    }

    @Override
    public Node getInfo(final NodeEndpoint endpoint) {
        final URL url = endpoint.getApiUrl(NodeApiId.REST_NODE_INFO);
        return new Node(this.httpMethodClient.get(url));
    }

    @Override
    public NodeCollection getKnownPeers(final NodeEndpoint endpoint) {
        final URL url = endpoint.getApiUrl(NodeApiId.REST_NODE_PEER_LIST);
        return new NodeCollection(this.httpMethodClient.get(url));
    }

    @Override
    public void announce(final NodeEndpoint endpoint, final NodeApiId announceId, final SerializableEntity entity) {
        final URL url = endpoint.getApiUrl(announceId);
        this.httpMethodClient.post(url, entity);
    }
}
