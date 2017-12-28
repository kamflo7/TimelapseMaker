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
import android.util.Log
import android.widget.Toast
import pl.kflorczyk.timelapsemaker.MainActivity
import pl.kflorczyk.timelapsemaker.Util
import pl.kflorczyk.timelapsemaker.Util.log
import pl.kflorczyk.timelapsemaker.bluetooth.messages.Messages
import pl.kflorczyk.timelapsemaker.timelapse.TimelapseService
import java.io.IOException

/**
 * Created by Kamil on 2017-12-25.
 */
class BluetoothClientService: Service() {
    private val TAG = "BluetoothClientService"

    companion object {
        val BT_CLIENT_TIMELAPSE_INITIALIZED: String = "btClientTimelapseInitialized"
        val BT_CLIENT_DO_CAPTURE: String = "btClientDoCapture"
        val BT_CLIENT_CAPTURED: String = "btClientCaptured"
    }

    private var connectedThread: ConnectedThread? = null

    private var mMessageReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            var msg = intent?.getStringExtra(MainActivity.BROADCAST_MSG)

            when(msg) {
                BT_CLIENT_TIMELAPSE_INITIALIZED -> connectedThread!!.write(Messages.build(Messages.MessageType.CLIENT_TIMELAPSE_INITIALIZED))
                BT_CLIENT_CAPTURED -> connectedThread!!.write(Messages.build(Messages.MessageType.CLIENT_CAPTURED))
            }

            log(TAG, "HttpServer got BroadcastMessage: $intent")
        }
    }

    private var handler: Handler = Handler(Handler.Callback { msg ->
        val msgType = msg.what
        val response = msg.obj as ConnectedThread.ResponseMessage

        when(msgType) {
            Messages.MessageType.SERVER_START_TIMELAPSE.ordinal -> {
                if(!Util.isMyServiceRunning(TimelapseService::class.java, applicationContext)) {
                    log(TAG, "[Handler] Msg obatined from server: SERVER_START_TIMELAPSE")
                    startService(Intent(applicationContext, TimelapseService::class.java))
                }

                Toast.makeText(applicationContext, "Server says to start Timelapse!", Toast.LENGTH_LONG).show()
            }
            Messages.MessageType.SERVER_DO_CAPTURE.ordinal -> {
                Util.broadcastMessage(applicationContext, BT_CLIENT_DO_CAPTURE)
                log(TAG, "[Handler] Msg obatined from server: BT_CLIENT_DO_CAPTURE")
                Toast.makeText(applicationContext, "Server said do capture!", Toast.LENGTH_SHORT).show()
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
        log(TAG, "Connected to the BluetoothServer, initializing input/output streams..")
        connectedThread = ConnectedThread(socket, this.handler)
        connectedThread!!.start()


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