package pl.kflorczyk.timelapsemaker.timelapse

import android.app.Service
import android.content.Intent
import android.os.IBinder

/**
 * Created by Kamil on 2017-12-09.
 */
class TimelapseService : Service() {

    override fun onCreate() {
        super.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY;
    }

    override fun onBind(p0: Intent?): IBinder {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}