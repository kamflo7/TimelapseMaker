package pl.kflorczyk.timelapsemaker.http_server

import fi.iki.elonen.NanoHTTPD

/**
 * Created by Kamil on 2017-12-16.
 */
class HttpServer(port: Int) : NanoHTTPD(port) {

    override fun serve(session: IHTTPSession?): Response {

        return newFixedLengthResponse("It works!")
    }
}