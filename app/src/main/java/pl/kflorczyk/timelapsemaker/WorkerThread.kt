package pl.kflorczyk.timelapsemaker

import android.os.Handler
import android.os.HandlerThread

/**
 * Created by Kamil on 2017-12-10.
 */
class WorkerThread(name: String) : HandlerThread(name) {
    var handler: Handler? = null

    fun waitUntilReady() {
        synchronized(this) {
            handler = Handler(looper)
        }
    }

}