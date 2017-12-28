package pl.kflorczyk.timelapsemaker.http_server

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import pl.kflorczyk.timelapsemaker.Util
import pl.kflorczyk.timelapsemaker.Util.log
import java.io.IOException

/**
 * Created by Kamil on 2017-12-16.
 */
class HttpService : Service() {
    private val TAG = "HttpService"

    companion object {
        val PORT: Int = 9090
    }

    private var server: HttpServer? = null

    override fun onCreate() {
        super.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        server = HttpServer(PORT, applicationContext)
        try {
            server!!.start()
            log(TAG, "onStartCommand() -> HttpServer.start() succesfully")
        } catch(e: IOException) {
            log(TAG, "onStartCommand() -> HttpServer.start() causes IOException")
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        server?.stop()
        log(TAG, "onDestroy()")

    }

    override fun onLowMemory() {
        super.onLowMemory()
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }
}