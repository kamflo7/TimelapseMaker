package com.loony.timelapsemaker.http_server;

import android.content.Context;

import com.loony.timelapsemaker.Util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

/**
 * Created by Kamil on 7/27/2017.
 */

public class HttpServer extends NanoHTTPD {
    private static String HTTP_AUTHORIZATION = "authorization";
    private static final String PASSWORD = "Basic YWRtaW46YmFyeWxvaw==";

    private Context context;

    public HttpServer(Context context, int port) {
        super(port);
        this.context = context;
        Util.log("HttpServer::__construct IP: " + Util.getLocalIpAddress(true) + ":"+port);
    }

    @Override
    public Response serve(IHTTPSession session) {
        //printSession(session);

        if(!isAuthorized(session)) {
            Response response = newFixedLengthResponse(Response.Status.UNAUTHORIZED, MIME_PLAINTEXT, "Need authentication");
            response.addHeader("WWW-Authenticate", "Basic realm=\"TimelapseMaker\"");
            return response;
        }

        String uri = session.getUri();

        if(uri.contains(".html") || uri.equals("/") || uri.isEmpty()) {
            uri = session.getUri();
            if(uri.equals("/") || uri.isEmpty()) uri = "/index.html";
            if(uri.equals("/index.html")) return serveHTML(session, uri);
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "Internal error");
        } else if(uri.contains(".jpg") || uri.contains(".png") || uri.contains(".ico")) {
            return serveInputStream(session, "image/jpeg");
        }  else if(uri.contains(".css")) {
            return serveInputStream(session, "text/css");
        } else if(uri.contains(".js")) {
            return serveInputStream(session, "text/javascript");
        }


        return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Page not found");
    }

    private Response serveInputStream(IHTTPSession session, String mimeType) {
        try {
            InputStream is = context.getAssets().open(session.getUri().substring(1));
            return newChunkedResponse(Response.Status.OK, mimeType, is);

        } catch (IOException e) {
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "Internal error");
        }
    }

    private Response serveHTML(IHTTPSession session, String uri) {
        String outputHTML="";

        InputStream is;
        try {
            is = context.getAssets().open(uri.substring(1));
            outputHTML = readInputStream(is);
            return newFixedLengthResponse(outputHTML);
        } catch (IOException e) {
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "Internal error");
        }
    }

    private boolean isAuthorized(IHTTPSession session) {
        Map<String, String> headers = session.getHeaders();
        if(headers.containsKey(HTTP_AUTHORIZATION) && headers.get(HTTP_AUTHORIZATION).equals(PASSWORD))
            return true;

        return false;
    }

    private String readInputStream(InputStream is) throws IOException {
        String output = "";

        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        String currentLine;

        while((currentLine = br.readLine()) != null) {
            output += currentLine;
        }

        br.close();

        return output;
    }

    private void printSession(IHTTPSession session) {
        Map<String, String> headers = session.getHeaders();
        String uri = session.getUri();
        String method = session.getMethod().name();

        String out = "{\"Headers\": [";
        for(Map.Entry<String, String> entry : headers.entrySet())
            out += String.format("{\"key\": \"%s\", \"value\": \"%s\"},", entry.getKey(), entry.getValue());
        out += "],";
        out += String.format("\"uri\": \"%s\",", uri);
        out += String.format("\"method\": \"%s\"}", method);
        Util.log("printSession -> " + out);
    }
}
