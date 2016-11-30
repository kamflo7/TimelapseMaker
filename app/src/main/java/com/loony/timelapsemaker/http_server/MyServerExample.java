package com.loony.timelapsemaker.http_server;

import java.io.IOException;

import fi.iki.elonen.NanoHTTPD;

import static com.loony.timelapsemaker.Util.log;

/**
 * Created by Kamil on 11/30/2016.
 */

public class MyServerExample extends NanoHTTPD {
    private final static int PORT = 8080;

    public MyServerExample() throws IOException {
        super(PORT);
        start();
        log("\nRunning! Point your browers to http://localhost:8080/ \n");
    }

    @Override
    public Response serve(IHTTPSession session) {
        String msg = "<html><body><h1>Hello server</h1>\n";
        msg += "<p>We serve " + session.getUri() + " !</p>";
        return newFixedLengthResponse( msg + "</body></html>\n" );
    }
}
