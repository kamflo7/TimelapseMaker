package pl.kflorczyk.timelapsemaker.bluetooth

import android.app.Service
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.IBinder
import android.support.v4.content.LocalBroadcastManager
import pl.kflorczyk.timelapsemaker.MainActivity
import pl.kflorczyk.timelapsemaker.Util
import pl.kflorczyk.timelapsemaker.Util.broadcastMessage
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.ObjectInputStream

/**
 * Created by Kamil on 2017-12-25.
 */
class BluetoothClientService: Service() {

    private var connectedThread: ConnectedThread? = null

    private var mMessageReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            var msg = intent?.getStringExtra(MainActivity.BROADCAST_MSG)

            Util.log("HttpServer got BroadcastMessage: $intent")
        }
    }

    private var handler: Handler = Handler(Handler.Callback { msg ->
        val msgType = msg.what
        val data = msg.obj as ByteArray

        when(msgType) {
            ConnectedThread.MESSAGE_READ -> {

                Util.log("BTClientService.Handler.onMessage type: $msgType, data: $data")
//                broadcastMessage(applicationContext, "client_is_getting_msg_from_server")
            }
            ConnectedThread.MESSAGE_WRITE -> {

            }
        }
        true
    })

    override fun onCreate() {
        super.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if(intent == null) {
            stopSelf()
            return START_STICKY
        }

        LocalBroadcastManager.getInstance(applicationContext).registerReceiver(mMessageReceiver, IntentFilter(MainActivity.BROADCAST_FILTER))

        var btDevice: BluetoothDevice = intent!!.extras.get("btDevice") as BluetoothDevice
        var socket: BluetoothSocket? = null

        try {
            socket = btDevice.createRfcommSocketToServiceRecord(BluetoothManager.uuid)
        } catch(e: IOException) {
            e.printStackTrace()
        }

        if(socket != null) {
            var r = Runnable {
                try {
                    socket.connect()
                } catch(connectException: IOException) {
                    connectException.printStackTrace()
                    try {
                        socket.close()
                    } catch(closeException: IOException) {
                        closeException.printStackTrace()
                    }
                }
                manageConnectedClient(socket)
            }
            Thread(r).start()
        }

        return START_STICKY
    }

    fun manageConnectedClient(socket: BluetoothSocket) {
        Util.log("[BluetoothClientService] Connected to the server, initializing input/output streams..")
        connectedThread = ConnectedThread(socket, this.handler)
        connectedThread!!.start()

//        Thread({
//            var x: Boolean = false
//
//            while(true) {
//                val byteArray = if (x) byteArrayOf(10, 20, 30, 40) else byteArrayOf(20, 40, 60, 80)
//                x = !x
//                connectedThread!!.write(byteArray)
//
//                Thread.sleep(4000)
//            }
//        }).start()
    }

    override fun onDestroy() {
        connectedThread?.cancel()
        connectedThread = null

        LocalBroadcastManager.getInstance(applicationContext).unregisterReceiver(mMessageReceiver)
    }

    override fun onLowMemory() {
        super.onLowMemory()
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }
}