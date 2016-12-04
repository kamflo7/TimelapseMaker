package com.loony.timelapsemaker.http_server;

import com.loony.timelapsemaker.http_server.api.NanoWSD;
import com.loony.timelapsemaker.http_server.api.WebSocket;

import java.security.NoSuchAlgorithmException;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.Response;
import fi.iki.elonen.NanoHTTPD.IHTTPSession;

import static com.loony.timelapsemaker.http_server.api.NanoWSD.makeAcceptKey;

/**
 * Created by Kamil on 12/2/2016.
 */

public class WebSocketResponseHandler {

    public static final String HEADER_UPGRADE = "upgrade";
    public static final String HEADER_UPGRADE_VALUE = "websocket";
    public static final String HEADER_CONNECTION = "connection";
    public static final String HEADER_CONNECTION_VALUE = "Upgrade";
    public static final String HEADER_WEBSOCKET_VERSION = "sec-websocket-version";
    public static final String HEADER_WEBSOCKET_VERSION_VALUE = "13";
    public static final String HEADER_WEBSOCKET_KEY = "sec-websocket-key";
    public static final String HEADER_WEBSOCKET_ACCEPT = "sec-websocket-accept";
    public static final String HEADER_WEBSOCKET_PROTOCOL = "sec-websocket-protocol";

    public WebSocketResponseHandler(IWebSocketFactory webSocketFactory) {
        this.webSocketFactory = webSocketFactory;
    }

    private final IWebSocketFactory webSocketFactory;

    public Response serve(final IHTTPSession session) {
        Map<String, String> headers = session.getHeaders();

        if(isWebSocketRequested(headers)) {
            if (!HEADER_WEBSOCKET_VERSION_VALUE.equalsIgnoreCase(headers.get(HEADER_WEBSOCKET_VERSION)))
                return NanoHTTPD.newFixedLengthResponse(Response.Status.BAD_REQUEST, NanoHTTPD.MIME_PLAINTEXT, "Invalid Websocket-Version " + headers.get(HEADER_WEBSOCKET_VERSION));

            if(!headers.containsKey(HEADER_WEBSOCKET_KEY))
                return NanoHTTPD.newFixedLengthResponse(Response.Status.BAD_REQUEST, NanoHTTPD.MIME_PLAINTEXT, "Missing WebSocket-Key");


            // ###
            WebSocket webSocket = webSocketFactory.openWebSocket(session);
            Response handshakeResponse = webSocket.getHandshakeResponse();
            try {
                handshakeResponse.addHeader(NanoWSD.HEADER_WEBSOCKET_ACCEPT, makeAcceptKey(headers.get(NanoWSD.HEADER_WEBSOCKET_KEY)));
            } catch (NoSuchAlgorithmException e) {
                return NanoHTTPD.newFixedLengthResponse(Response.Status.INTERNAL_ERROR, NanoHTTPD.MIME_PLAINTEXT,
                        "The SHA-1 Algorithm required for websockets is not available on the server.");
            }

            if (headers.containsKey(NanoWSD.HEADER_WEBSOCKET_PROTOCOL)) {
                handshakeResponse.addHeader(NanoWSD.HEADER_WEBSOCKET_PROTOCOL, headers.get(NanoWSD.HEADER_WEBSOCKET_PROTOCOL).split(",")[0]);
            }
            // ###

            return handshakeResponse;
        } else {
            return null;
        }
    }

    private boolean isWebSocketRequested(Map<String, String> headers) {
        String upgrade = headers.get(HEADER_UPGRADE);
        boolean isCorrectConnection = isWebSocketConnectionHeader(headers);
        boolean isUpgrade = HEADER_UPGRADE_VALUE.equalsIgnoreCase(upgrade);
        return (isUpgrade && isCorrectConnection);
    }

    private boolean isWebSocketConnectionHeader(Map<String, String> headers) {
        String connection = headers.get(HEADER_CONNECTION);
        return (connection != null && connection.toLowerCase().contains(HEADER_CONNECTION_VALUE.toLowerCase()));
    }

}
