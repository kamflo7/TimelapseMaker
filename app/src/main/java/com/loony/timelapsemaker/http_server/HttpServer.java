package com.loony.timelapsemaker.http_server;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Base64;
import android.widget.Toast;

import com.loony.timelapsemaker.NewActivity;
import com.loony.timelapsemaker.Util;
import com.loony.timelapsemaker.camera.Resolution;
import com.loony.timelapsemaker.camera.TimelapseConfig;

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
//    private static String BROADCAST_FILTER
    private static String HTTP_AUTHORIZATION = "authorization";
    private static final String PASSWORD = "Basic YWRtaW46YmFyeWxvaw==";

    private Context context;

    // dispatch values
    private String base64image = null;
//    private JSONObject dataJson = null;

    private Resolution resolution;
    private int intervalMilisecond;
    private int capturedPhotos;
    private int maxPhotos;

    private long timeOfLastPhotoCapture;

    public HttpServer(Context context, int port, TimelapseConfig timelapseConfig) {
        super(port);
        this.context = context;
//        makeBase64image(testLoadImageFromDisk());
        //Util.log("TestBufferSize: " + (testImgBuffer != null ? testImgBuffer.length : "null"));
        Util.log("HttpServer::__construct IP: " + Util.getLocalIpAddress(true) + ":"+port);
        LocalBroadcastManager.getInstance(context).registerReceiver(mMessageReceiver, new IntentFilter(Util.BROADCAST_FILTER));
        resolution = timelapseConfig.getPictureSize();
        intervalMilisecond = (int) timelapseConfig.getMilisecondsInterval();
        maxPhotos = timelapseConfig.getPhotosLimit();
    }

//    private byte[] testLoadImageFromDisk() {
//        InputStream is = null;
//        byte[] buffer = null;
//        try {
//            is = context.getAssets().open("toSend.jpg");
//            buffer = new byte[is.available()];
//            is.read(buffer);
//            is.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//        return buffer;
//    }

    private void makeBase64image(byte[] sourceBytes) {
        Bitmap bm = BitmapFactory.decodeByteArray(sourceBytes, 0, sourceBytes.length);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        bm.compress(Bitmap.CompressFormat.JPEG, 60, outputStream);
        byte[] b = outputStream.toByteArray();

        //JSONObject o = new JSONObject();
        //o.put("image", Base64.encodeToString(b, Base64.NO_WRAP));
//            base64image = o.toString();
        base64image = Base64.encodeToString(b, Base64.NO_WRAP);

        Util.log("Made base64 image. Compressed from %dKB to %dKB, reduced %.1f%%", (int) (sourceBytes.length/1024f), (int) (b.length/1024f), ((sourceBytes.length-b.length)/(float) sourceBytes.length)*100f);
    }

//    private void makeJsonData() {
//        dataJson = new JSONObject();
//        try {
//            dataJson.put("resolution", resolution != null ? (String.format("%dx%d", resolution.getWidth(), resolution.getHeight())) : "unknow");
//            dataJson.put("intervalMiliseconds", intervalMilisecond);
//            dataJson.put("capturedPhotos", capturedPhotos);
//            dataJson.put("maxPhotos", maxPhotos);
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }
//    }

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String msg = intent.getStringExtra(Util.BROADCAST_MESSAGE);
            if(msg != null) {
                if(msg.equals(Util.BROADCAST_MESSAGE_CAPTURED_PHOTO)) {
                    Util.log("HttpServer::Broadcast received a message");
//                    lastPhotoTakenAtMilisTime = System.currentTimeMillis();
                    capturedPhotos = intent.getIntExtra(Util.BROADCAST_MESSAGE_CAPTURED_PHOTO_AMOUNT, -1);
                    timeOfLastPhotoCapture = System.currentTimeMillis();
                    byte[] image = intent.getByteArrayExtra("imageBytes");
                    makeBase64image(image);
                }
            }
        }
    };

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
        } else if(uri.equals("/getData")) {
            return serveData();
        }


        return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Page not found");
    }

    private Response serveData() {
        JSONObject dataJson = new JSONObject();

        try {
            dataJson.put("resolution", resolution != null ? (String.format("%dx%d", resolution.getWidth(), resolution.getHeight())) : "unknow")
                    .put("intervalMiliseconds", intervalMilisecond)
                    .put("capturedPhotos", capturedPhotos)
                    .put("maxPhotos", maxPhotos)
                    .put("timeMsToNextCapture", intervalMilisecond - (System.currentTimeMillis() - timeOfLastPhotoCapture))
                    .put("image", base64image);
        } catch (JSONException e) {
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "Internal error");
        }


        String response = dataJson.toString();

        Util.log("Someone is requesting data, data: " + response);
        return newFixedLengthResponse(response);
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

    private Intent getSendingMessageIntent(String message) {
        Intent intent = new Intent(Util.BROADCAST_FILTER);
        intent.putExtra(Util.BROADCAST_MESSAGE, message);
        return intent;
    }
}
