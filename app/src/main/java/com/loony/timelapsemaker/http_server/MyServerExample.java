package com.loony.timelapsemaker.http_server;

import android.content.Context;

import com.loony.timelapsemaker.R;
import com.loony.timelapsemaker.Util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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

    public MyServerExample(Context context) throws IOException {
        super(PORT);
        this.context = context;
        start();
        log("\nRunning! Point your browers to http://localhost:8080/ \n");
    }

    private Random random = new Random();
    private String getRandomPokemon() {
        String[] pokemons = new String[] { "Bulbasaur", "Ivysaur", "Venusaur", "Charmander","Charmeleon",  "Charizard", "Squirtle"};
        int choice = random.nextInt(pokemons.length);
        return pokemons[choice];
    }

    @Override
    public Response serve(IHTTPSession session) {
        Map<String, String> params = session.getParms();

        String outputHTML="";

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


        return newFixedLengthResponse(outputHTML);
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


//        String msg = "<html><body><h1>Hello server</h1>\n";
//        msg += "<p>session.getUri(): '" + session.getUri() + "' !</p>";
//
//        msg += "<p>Headers:</p><ul>";
//        Map<String, String> headers = session.getHeaders();
//        for(Map.Entry<String, String> entry : headers.entrySet())
//            msg += String.format("<li>'%s' -> '%s'</li>", entry.getKey(), entry.getValue());
//        msg += "</ul>";
//
//        Map<String, String> params = session.getParms();
//        boolean paramsExists = params != null;
//        msg += String.format("<p>Params: [boolean: %b; other: %s]</p><ul>", paramsExists, paramsExists ? "size: "+params.size() : "size 0");
//        for(Map.Entry<String, String> entry : params.entrySet())
//            msg += String.format("<li>'%s' -> '%s'</li>", entry.getKey(), entry.getValue());
//        msg += "</ul>";
//
//        String queryParam = session.getQueryParameterString();
//        msg += "<p>session.getQueryParameterString(): '" + queryParam + "'</p>";
//        msg += "</body></html>\n";
