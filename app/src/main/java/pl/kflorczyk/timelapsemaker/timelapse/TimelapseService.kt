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

//            synchronized(this@TimelapseService) {
//                if(haveToStopSignal) {
//                    return@Runnable
//                }
//            }

            var listener = object : OnTimelapseStateChangeListener {
                override fun onInit() {
                    Util.log("[Service] got onInit")
                    TimelapseController.capturePhoto()
                }

                override fun onCapture(bytes: ByteArray?) {
                    Util.log("[Service] Got photo")
                }

                override fun onFail(msg: String) {
                    Util.log("[Service] Got onFail")
                }
            }

            try {
                TimelapseController.startTimelapse(listener, this@TimelapseService.applicationContext)
            } catch(e: RuntimeException) {
                Util.log("[TimelapseService] TimelapseController.startTimelapse caught exception ${e.message}");
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