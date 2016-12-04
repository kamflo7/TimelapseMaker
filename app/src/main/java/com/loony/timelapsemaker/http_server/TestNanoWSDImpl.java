package com.loony.timelapsemaker.http_server;

import com.loony.timelapsemaker.http_server.api.NanoWSD;
import com.loony.timelapsemaker.http_server.api.WebSocket;

import fi.iki.elonen.NanoHTTPD;

/**
 * Created by Kamil on 12/4/2016.
 */

@Deprecated
public class TestNanoWSDImpl extends NanoWSD {
    public TestNanoWSDImpl(int port) {
        super(port);
    }

    public TestNanoWSDImpl(String hostname, int port) {
        super(hostname, port);
    }

    @Override
    protected WebSocket openWebSocket(IHTTPSession handshake) {
        return null;
    }
}
