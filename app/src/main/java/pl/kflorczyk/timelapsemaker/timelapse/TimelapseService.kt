package pl.kflorczyk.timelapsemaker.timelapse

import android.app.Service
import android.content.Intent
import android.os.IBinder
import pl.kflorczyk.timelapsemaker.Util
import pl.kflorczyk.timelapsemaker.WorkerThread
import android.support.v4.content.LocalBroadcastManager
import pl.kflorczyk.timelapsemaker.MainActivity


/**
 * Created by Kamil on 2017-12-09.
 */
class TimelapseService : Service() {

    private var worker: WorkerThread? = null

    override fun onCreate() {
        super.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        var runnable = Runnable {
            var listener = object : TimelapseController.OnTimelapseProgressListener {
                override fun onCapture(bytes: ByteArray?) {
                    val i = getSendingMessageIntent(MainActivity.BROADCAST_MESSAGE_CAPTURED_PHOTO)
                    i.putExtra(MainActivity.BROADCAST_MESSAGE_CAPTURED_PHOTO_BYTES, bytes)
                    LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(i)
                }

                override fun onFail(msg: String?) {
                    val i = getSendingMessageIntent(MainActivity.BROADCAST_MESSAGE_FAILED)
                    LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(i)
                    this@TimelapseService.stopSelf()
                }

                override fun onComplete() {
                    val i = getSendingMessageIntent(MainActivity.BROADCAST_MESSAGE_COMPLETE)
                    LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(i)
                    this@TimelapseService.stopSelf()
                }
            }

            try {
                TimelapseController.startTimelapse(listener, this@TimelapseService.applicationContext)
            } catch(e: RuntimeException) {
                val i = getSendingMessageIntent(MainActivity.BROADCAST_MESSAGE_FAILED)
                LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(i)

                this@TimelapseService.stopSelf()
                Util.log("[TimelapseService] TimelapseController.startTimelapse caught exception ${e.message}");
            }
        }

        worker = WorkerThread("ServiceThread")
        worker!!.start()
        worker!!.waitUntilReady()
        worker!!.handler!!.post(runnable)

        return START_STICKY;
    }

    override fun onDestroy() {
        super.onDestroy()

        worker!!.handler = null
        worker!!.quit()
        worker!!.interrupt()
        worker = null
    }

    override fun onLowMemory() {
        super.onLowMemory()
    }


    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    private fun getSendingMessageIntent(message: String): Intent {
        val intent = Intent(MainActivity.BROADCAST_FILTER)
        intent.putExtra(MainActivity.BROADCAST_MSG, message)
        return intent
    }
}