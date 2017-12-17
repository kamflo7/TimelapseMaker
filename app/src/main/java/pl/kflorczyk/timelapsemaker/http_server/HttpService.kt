package pl.kflorczyk.timelapsemaker.http_server

import android.app.Service
import android.content.Intent
import android.os.IBinder
import pl.kflorczyk.timelapsemaker.Util
import java.io.IOException

/**
 * Created by Kamil on 2017-12-16.
 */
class HttpService : Service() {
    companion object {
        val PORT: Int = 9090
    }

    private var server: HttpServer? = null

    override fun onCreate() {
        super.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        server = HttpServer(PORT)
        try {
            server!!.start()
        } catch(e: IOException) {

        }
        Util.log("HttpService started!")

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        server?.stop()
        Util.log("HttpService is going down!")

    }

    override fun onLowMemory() {
        super.onLowMemory()
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }
}