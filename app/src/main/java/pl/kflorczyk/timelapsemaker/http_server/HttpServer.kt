package pl.kflorczyk.timelapsemaker.http_server

import android.content.Context
import fi.iki.elonen.NanoHTTPD
import java.io.IOException
import java.io.InputStream
import java.io.BufferedReader
import java.io.InputStreamReader


/**
 * Created by Kamil on 2017-12-16.
 */
class HttpServer(port: Int, context: Context) : NanoHTTPD(port) {

    val context: Context = context

    override fun serve(session: IHTTPSession): Response {

        var uri = session.uri

        if(uri.contains(".html") || uri.equals("/") || uri.isEmpty()) {
            if(uri == "/" || uri.isEmpty()) {
                uri = "/index.html"
            }

            if(uri == "/index.html") {
                return serveHTML(session, uri)
            }
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "Internal error")
        } else if(uri.contains(".jpg") || uri.contains(".png") || uri.contains(".ico")) {
            return serveInputStream(session, "image/jpeg");
        }  else if(uri.contains(".css")) {
            return serveInputStream(session, "text/css");
        } else if(uri.contains(".js")) {
            return serveInputStream(session, "text/javascript");
        }

        return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "Internal error")
    }

    private fun serveHTML(session: IHTTPSession, uri: String): Response {
        var outputHTML: String?

        val inputStream: InputStream
        try {
            inputStream = context.getAssets().open(uri.substring(1))
            outputHTML = readInputStream(inputStream)
            return NanoHTTPD.newFixedLengthResponse(outputHTML)
        } catch (e: IOException) {
            return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.INTERNAL_ERROR, NanoHTTPD.MIME_PLAINTEXT, "Internal error")
        }
    }

    private fun serveInputStream(session: NanoHTTPD.IHTTPSession, mimeType: String): NanoHTTPD.Response {
        try {
            val inputStream = context.assets.open(session.uri.substring(1))
            return NanoHTTPD.newChunkedResponse(NanoHTTPD.Response.Status.OK, mimeType, inputStream)
        } catch (e: IOException) {
            return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.INTERNAL_ERROR, NanoHTTPD.MIME_PLAINTEXT, "Internal error")
        }
    }

    @Throws(IOException::class)
    private fun readInputStream(inputStream: InputStream): String {
        var output = StringBuilder()

        val br = BufferedReader(InputStreamReader(inputStream))
        var currentLine: String?

        while (true) {
            currentLine = br.readLine()
            if(currentLine == null)
                break

            output.appendln(currentLine)// += currentLine
        }

        br.close()
        return output.toString()
    }
}