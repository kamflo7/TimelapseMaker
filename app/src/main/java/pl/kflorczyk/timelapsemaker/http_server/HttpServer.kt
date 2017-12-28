package pl.kflorczyk.timelapsemaker.http_server

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.support.v4.content.LocalBroadcastManager
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.RelativeLayout
import fi.iki.elonen.NanoHTTPD
import org.json.JSONArray
import org.json.JSONObject
import pl.kflorczyk.timelapsemaker.*
import pl.kflorczyk.timelapsemaker.Util.broadcastMessage
import pl.kflorczyk.timelapsemaker.Util.log
import pl.kflorczyk.timelapsemaker.camera.CameraVersionAPI
import pl.kflorczyk.timelapsemaker.timelapse.TimelapseController
import pl.kflorczyk.timelapsemaker.timelapse.TimelapseService
import pl.kflorczyk.timelapsemaker.timelapse.TimelapseSettings
import java.io.IOException
import java.io.InputStream
import java.io.BufferedReader
import java.io.InputStreamReader


/**
 * Created by Kamil on 2017-12-16.
 */
class HttpServer(port: Int, context: Context) : NanoHTTPD(port) {
    private val TAG = "HttpServer"

    val context: Context = context
    var timelapseSettings: TimelapseSettings = (context.applicationContext as MyApplication).timelapseSettings!!

    private var mMessageReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            var msg = intent?.getStringExtra(MainActivity.BROADCAST_MSG)

            log(TAG, "[MessageReceiver] message: $msg; intent: $intent")
        }
    }

    init {
        LocalBroadcastManager.getInstance(this.context).registerReceiver(mMessageReceiver, IntentFilter(MainActivity.BROADCAST_FILTER))
    }

    override fun stop() {
        LocalBroadcastManager.getInstance(this.context).unregisterReceiver(mMessageReceiver)
    }

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
            return serveInputStream(session, "image/jpeg")
        }  else if(uri.contains(".css")) {
            return serveInputStream(session, "text/css")
        } else if(uri.contains(".js")) {
            return serveInputStream(session, "text/javascript")
        }

        if(uri.contains("settings")) {
            if(session.method == Method.PUT) {
                return handleSettingsEdit(session)
            }
        } else if(uri.contains("hardware") && session.method == Method.GET) {
            return handleHardware(session)
        } else if(uri.contains("state") && session.method == Method.GET) {

        } else if(uri.contains("startTimelapse")) {
            return handleStartTimelapse(session)
        } else if(uri.contains("stopTimelapse")) {
            return handleStopTimelapse(session)
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

    private fun getSingleParameter(name:String, map: Map<String, List<String>>): String? {
        val value = map.getOrElse(name, { null })
        if(value != null) {
            if(value[0].isEmpty())
                return null
            else
                return value[0]
        }
        return null
    }

    private fun handleStopTimelapse(session: IHTTPSession): Response {
        if(TimelapseController.getState() != TimelapseController.State.TIMELAPSE)
            return newFixedLengthResponse(MyResponse(MyResponse.ResponseStatus.FAIL, "Timelapse is not currently created", null).build())

        context.stopService(Intent(context, TimelapseService::class.java))
        broadcastMessage(this@HttpServer.context, MainActivity.BROADCAST_MSG_REMOTE_STOP_TIMELAPSE)
        return newFixedLengthResponse(MyResponse(MyResponse.ResponseStatus.OK, null, null).build())
    }

    private fun handleStartTimelapse(session: IHTTPSession): Response {
        if(TimelapseController.getState() == TimelapseController.State.TIMELAPSE)
            return newFixedLengthResponse(MyResponse(MyResponse.ResponseStatus.FAIL, "Timelapse is currently created", null).build())

        TimelapseController.stopPreview()

        if(Util.isMyServiceRunning(TimelapseService::class.java, context)) {
            context.stopService(Intent(context, TimelapseService::class.java))
        }

        context.startService(Intent(context, TimelapseService::class.java))
        broadcastMessage(this@HttpServer.context, MainActivity.BROADCAST_MSG_REMOTE_START_TIMELAPSE)
        return newFixedLengthResponse(MyResponse(MyResponse.ResponseStatus.OK, null, null).build())
    }

    private fun handleHardware(session: IHTTPSession): Response {
        val availableResolutions = timelapseSettings.availableResolutions
        val cameras = Util.getAvailableCameraAPI().map { c -> if(c == CameraVersionAPI.V_1) "API 1" else "API 2" }
        val storages = StorageManager.getStorages(context)

        var o = JSONObject()
        var resolutionsJson = JSONArray()
        availableResolutions.forEach { resolution -> resolutionsJson.put(resolution) }
        o.put("resolutions", resolutionsJson)

        var cameraapisJson = JSONArray()
        cameras.forEach { camera -> cameraapisJson.put(camera) }
        o.put("camera_api", cameraapisJson)

        var storagesJson = JSONArray()
        storages.forEach { storage -> storagesJson.put(storage.second.name) }
        o.put("storages", storagesJson)

        val myResponse = MyResponse(MyResponse.ResponseStatus.OK, "From one device", o)
        return newFixedLengthResponse(myResponse.build())
    }

    private fun handleSettingsEdit(session: IHTTPSession): Response {
        val property = getSingleParameter("property", session.parameters)
        val value = getSingleParameter("value", session.parameters)
        val deviceuuid = getSingleParameter("deviceuuid", session.parameters)

        if(property == null || value == null) {
            return newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, "Incorrect parameters")
        }

        if(TimelapseController.getState() == TimelapseController.State.TIMELAPSE) {
            return newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, "Cannot set TimelapseSettings while timelapse is doing")
        }

        when(property) {
            "frequencyCapturing" -> {
                val frequency = value.toLong()
                timelapseSettings.frequencyCapturing = frequency

                broadcastMessage(this@HttpServer.context, MainActivity.BROADCAST_MSG_REMOTE_EDIT_SETTINGS)
            }
            "resolution" -> {
                var parts: List<String> = value.split("x")
                if(parts.size == 2) {
                    val width = parts[0].toInt()
                    val height = parts[1].toInt()

                    val resolution = timelapseSettings.availableResolutions.find { r -> r.width == width && r.height == height }
                    if(resolution != null) {
                        timelapseSettings.resolution = resolution
                        broadcastMessage(this@HttpServer.context, MainActivity.BROADCAST_MSG_REMOTE_EDIT_SETTINGS, "resolution")
                        return newFixedLengthResponse("Ok")
                    }
                }
                return newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, "")
            }
            "cameraVersion" -> {
                return newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, "")
            }
            "cameraId" -> {
                return newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, "")
            }
            "storageType" -> {
                return newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, "")
            }
        }



        return newFixedLengthResponse("Ok")
    }
}