package pl.kflorczyk.timelapsemaker.bluetooth

import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.os.IBinder
import pl.kflorczyk.timelapsemaker.Util
import pl.kflorczyk.timelapsemaker.WorkerThread
import java.io.IOException
import java.util.*

/**
 * Created by Kamil on 2017-12-20.
 */
class BluetoothService: Service() {

    private var worker: WorkerThread? = null

    override fun onCreate() {
        super.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Util.log("BluetoothService::onStartCommand!")
        var btAdapter = BluetoothAdapter.getDefaultAdapter()
        var serverSocket: BluetoothServerSocket? = null

        try {
            serverSocket = btAdapter.listenUsingRfcommWithServiceRecord("BTServer", BluetoothManager.uuid)
        } catch(e: IOException) {
            e.printStackTrace()
            stopSelf()
            return START_STICKY
        }

        val runnable = Runnable {
            while(true) {
                var socket: BluetoothSocket

                try {
                    socket = serverSocket.accept()
                } catch(e: IOException) {
                    Util.log("serverSocket.accept() failed")
                    break
                }

                Util.log("[BT] POLACZONO Z: $socket")
            }
        }

        worker = WorkerThread("BluetoothServiceServerThread")
        worker!!.start()
        worker!!.waitUntilReady()
        worker!!.handler!!.post(runnable)

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }
}