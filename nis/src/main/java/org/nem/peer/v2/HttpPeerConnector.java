package org.nem.peer.v2;

import net.minidev.json.*;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.*;
import org.eclipse.jetty.client.util.InputStreamResponseListener;
import org.nem.core.serialization.*;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;

/**
 * An HTTP-based PeerConnector implementation.
 */
public class HttpPeerConnector implements PeerConnector {

    private static final int HTTP_STATUS_OK = 200;
    private static final int DEFAULT_TIMEOUT = 30;

    private final HttpClient httpClient;

    /**
     * Creates a new HTTP peer connector.
     */
    public HttpPeerConnector() {
        try {
            this.httpClient = new HttpClient();
            this.httpClient.setFollowRedirects(false);
            this.httpClient.start();
        }
        catch (Exception ex) {
            throw new FatalPeerException("HTTP client could not be started", ex);
        }
    }

    @Override
    public Node getInfo(final NodeEndpoint endpoint) {
        final URL url = endpoint.getApiUrl(NodeApiId.REST_NODE_INFO);
        return new Node(this.getResponse(url));
    }

    @Override
    public NodeStatusDemux getKnownPeers(final NodeEndpoint endpoint) {
        final URL url = endpoint.getApiUrl(NodeApiId.REST_NODE_PEER_LIST);
        return new NodeStatusDemux(this.getResponse(url));
    }

    private JsonDeserializer getResponse(final URL url) {
        try {
            final URI uri = url.toURI();
            final InputStreamResponseListener listener = new InputStreamResponseListener();

            final Request req = this.httpClient.newRequest(uri);
            req.send(listener);

            Response res = listener.get(DEFAULT_TIMEOUT, TimeUnit.SECONDS);
            if (res.getStatus() != HTTP_STATUS_OK)
                return null;

            try (InputStream responseStream = listener.getInputStream()) {
                return new JsonDeserializer(
                    (JSONObject)JSONValue.parse(responseStream),
                    new DeserializationContext(null));
            }
        }
        catch (TimeoutException e) {
            throw new InactivePeerException(e);
        }
        catch (URISyntaxException|InterruptedException|ExecutionException|IOException e) {
            throw new FatalPeerException(e);
        }
    }
}
