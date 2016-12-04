package com.loony.timelapsemaker.http_server;

import android.content.Context;
import com.loony.timelapsemaker.R;
import com.loony.timelapsemaker.Util;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import java.util.Random;
import fi.iki.elonen.NanoHTTPD;
import static com.loony.timelapsemaker.Util.log;
import static com.loony.timelapsemaker.Util.mapToString;

/**
 * Created by Kamil on 11/30/2016.
 */

public class MyServerExample extends NanoHTTPD {
    private final static int PORT = 8080;
    private Context context;

    public MyServerExample(Context context) throws IOException {
        super(PORT);
        this.context = context;
        String ip = Util.getLocalIpAddress(true);
        log(String.format("\nRunning! Point your browers to %s:8080/ \n", ip != null ? ip : "problem"));
        start();
    }

    private Random random = new Random();
    private String getRandomPokemon() {
        String[] pokemons = new String[] { "Bulbasaur", "Ivysaur", "Venusaur", "Charmander","Charmeleon",  "Charizard", "Squirtle"};
        int choice = random.nextInt(pokemons.length);
        return pokemons[choice];
    }

    @Override
    public Response serve(IHTTPSession session) {
//        Util.log("Thread httpd serveera: " + Thread.currentThread().toString());
//        printHeaders(session.getHeaders());
//        printParameters(session.getParameters());

//        Util.log("URI: '%s'; uri is null: %s; uri len %d; isEmpty: %s",
//                session.getUri(), Boolean.toString(session.getUri() == null), session.getUri().length(), Boolean.toString(session.getUri().isEmpty()));

        String uri = session.getUri();
        if(uri.contains(".jpg") || uri.contains(".png") || uri.contains(".ico")) {
            return serveImages(session, uri);
        } else if(uri.contains(".html") || uri.equals("/") || uri.isEmpty()) {
            return servePage(session, uri);
        } else if(uri.contains(".api")) {
            return serveAPI(session, uri);
        }

        Util.log("SHOULD NEVER HAPPEN");
        return null;
    }

    private Response serveImages(IHTTPSession session, String uri) {
        try {
            InputStream is = context.getAssets().open(uri.substring(1));
            return newChunkedResponse(Response.Status.OK, "image/jpeg", is);

        } catch (IOException e) {
            return null;
        }
    }

    private Response serveAPI(IHTTPSession session, String uri) {
        if(uri.contains("pokemon.api")) {
            return newFixedLengthResponse(getRandomPokemon());
        }

        return null;
    }

    private Response servePage(IHTTPSession session, String uri) {
        if(uri.equals("/") || uri.isEmpty())
            uri = "/index.html";

        if(uri.equals("/index.html"))
            return serveHTML(session, uri);

        return null;
    }

    private Response serveHTML(IHTTPSession session, String uri) {
        String outputHTML="";

        InputStream is;
        try {
            is = context.getAssets().open(uri.substring(1));
            outputHTML = readInputStream(is);
            return newFixedLengthResponse(outputHTML);
        } catch (IOException e) {
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "IOException while serving");
        }
    }

    private static int i;
    private void printHeaders(Map<String, String> map) {
        String s = String.format("{'Request': '%d', 'Headers': {", i++);
        for(Map.Entry<String, String> entry : map.entrySet())
            s += String.format("'%s': '%s',", entry.getKey(), entry.getValue());
        s += "}}";
        Util.log(s);
    }

    private void printParameters(Map<String, List<String>> parameters) {
        Util.log("Parameters: ");

        for(Map.Entry<String, List<String>> entry : parameters.entrySet())
            Util.log("Param '%s' -> '%s'", entry.getKey(), entry.getValue());
    }

    private String readResource(int id) throws IOException {
        InputStream is = context.getResources().openRawResource(id);
//        String output = "";
//
//        try(BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
//            String currentLine;
//
//            while((currentLine = br.readLine()) != null) {
//                output += currentLine;
//            }
//        }
//        return output;
        return readInputStream(is);
    }

    private String readInputStream(InputStream is) throws IOException {
        String output = "";

        try(BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
            String currentLine;

            while((currentLine = br.readLine()) != null) {
                output += currentLine;
            }
        }
        return output;
    }
}