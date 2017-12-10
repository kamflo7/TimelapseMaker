package pl.kflorczyk.timelapsemaker.timelapse

import android.app.Service
import android.content.Intent
import android.os.IBinder
import pl.kflorczyk.timelapsemaker.Util
import pl.kflorczyk.timelapsemaker.WorkerThread

/**
 * Created by Kamil on 2017-12-09.
 */
class TimelapseService : Service() {

    private var haveToStopSignal: Boolean = false

    override fun onCreate() {
        super.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        var runnable = Runnable {
            for(i in 0..100) {
                synchronized(this@TimelapseService) {
                    if(haveToStopSignal) {
                        return@Runnable
                    }
                }

                Util.log("Thread counter $i")
                Thread.sleep(1000)
            }
        }

        var worker = WorkerThread("ServiceThread")
        worker.start()
        worker.waitUntilReady()
        worker.handler.post(runnable)

        return START_STICKY;
    }

    override fun onDestroy() {
        super.onDestroy()

        synchronized(this) {
            haveToStopSignal = true
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()
    }


    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

}