package com.loony.timelapsemaker.http_server;

import android.content.Context;

import com.loony.timelapsemaker.R;
import com.loony.timelapsemaker.Util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Random;

import fi.iki.elonen.NanoHTTPD;

import static com.loony.timelapsemaker.Util.log;

/**
 * Created by Kamil on 11/30/2016.
 */

public class MyServerExample extends NanoHTTPD {
    private final static int PORT = 8080;
    private Context context;
    private WebSocketResponseHandler responseHandler;

    public MyServerExample(Context context) throws IOException {
        super(PORT);
        this.context = context;
//        responseHandler = new WebSocketResponseHandler(webSocketFactory);
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
        printHeaders(session.getHeaders());
        String outputHTML="";

        Map<String, String> params = session.getParms();
        if(params.size() == 0) { // index
            try {
                outputHTML = readResource(R.raw.page);
            }
            catch (IOException e) {
                Util.log("MyServerExample::serve() -> IOException!");
                return newFixedLengthResponse("IOException, error 403 albo 500 albo jakis inny, nie znam sie na tych kodach");
            }
        } else {
            if(params.containsKey("pokemon")) {
                outputHTML = getRandomPokemon();
            }
        }

//        printSessionThings(session);
        return newFixedLengthResponse(outputHTML);
    }

    private static int i;
    private void printHeaders(Map<String, String> map) {
        String s = String.format("{'Request': '%d', 'Headers': {", i++);
        for(Map.Entry<String, String> entry : map.entrySet())
            s += String.format("'%s': '%s',", entry.getKey(), entry.getValue());
        s += "}}";
        Util.log(s);
    }

    private String readResource(int id) throws IOException {
        InputStream is = context.getResources().openRawResource(id);
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