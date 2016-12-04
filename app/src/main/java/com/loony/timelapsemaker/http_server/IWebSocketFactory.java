package com.loony.timelapsemaker.http_server;

import com.loony.timelapsemaker.http_server.api.WebSocket;

import fi.iki.elonen.NanoHTTPD;

/**
 * Created by Kamil on 12/4/2016.
 */

public interface IWebSocketFactory {
    WebSocket openWebSocket(NanoHTTPD.IHTTPSession handshake);
}
