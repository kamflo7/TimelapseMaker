package com.loony.timelapsemaker.http_server;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;

import com.loony.timelapsemaker.Util;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
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

    private String base64image = null;

    public HttpServer(Context context, int port) {
        super(port);
        this.context = context;
        makeBase64fromBytes();
        //Util.log("TestBufferSize: " + (testImgBuffer != null ? testImgBuffer.length : "null"));
        Util.log("HttpServer::__construct IP: " + Util.getLocalIpAddress(true) + ":"+port);
    }

    private void makeBase64fromBytes() {
        try {
            InputStream is = context.getAssets().open("toSend.jpg");
            byte[] testImgBuffer = new byte[is.available()];
            is.read(testImgBuffer);
            is.close();

            Bitmap bm = BitmapFactory.decodeByteArray(testImgBuffer, 0, testImgBuffer.length);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            bm.compress(Bitmap.CompressFormat.JPEG, 60, outputStream);
            byte[] b = outputStream.toByteArray();

            JSONObject o = new JSONObject();
            o.put("image", Base64.encodeToString(b, Base64.NO_WRAP));

            base64image = o.toString();

            Util.log("made base64 image. Compressed from %dKB to %dKB, reduced %.1f%%", (int) (testImgBuffer.length/1024f), (int) (b.length/1024f), ((testImgBuffer.length-b.length)/(float) testImgBuffer.length)*100f);
        } catch (IOException e) {
            e.printStackTrace();
            base64image = null;
        } catch (JSONException e) {
            e.printStackTrace();
            base64image = null;
        }
    }

    @Override
    public Response serve(IHTTPSession session) {
        //printSession(session);

        if(!isAuthorized(session)) {
            Response response = newFixedLengthResponse(Response.Status.UNAUTHORIZED, MIME_PLAINTEXT, "Needs authentication");
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
        } else if(uri.equals("/getImage")) {
            return serveCapturedImage();
        }


        return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Page not found");
    }

    private Response serveCapturedImage() {
        if(base64image == null)
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "Internal error");

        return newFixedLengthResponse(base64image);
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
