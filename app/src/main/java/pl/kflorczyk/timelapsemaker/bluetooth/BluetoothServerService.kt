package pl.kflorczyk.timelapsemaker.bluetooth

import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothServerSocket
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
import pl.kflorczyk.timelapsemaker.WorkerThread
import pl.kflorczyk.timelapsemaker.bluetooth.messages.Messages
import java.io.IOException
import kotlin.collections.ArrayList

/**
 * Created by Kamil on 2017-12-20.
 */
class BluetoothServerService : Service() {
    companion object {
        val BT_SERVER_TIMELAPSE_START: String = "btServerStartTimelapse"
        val BT_SERVER_CLIENTS_READY: String = "btServerClientsReady"
        val BT_SERVER_DO_CAPTURE: String = "btServerDoCapture"
    }

    private var worker: WorkerThread? = null
    private var connectedThreads: ArrayList<ConnectedThread> = ArrayList()
    private var clientsReadyForCapture = 0

    private var mMessageReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            var msg = intent?.getStringExtra(MainActivity.BROADCAST_MSG)

            when(msg) {
                BT_SERVER_TIMELAPSE_START -> {
                    clientsReadyForCapture = 0
                    val msgBytes = Messages.build(Messages.MessageType.SERVER_START_TIMELAPSE, null)

                    for(connectedThread in connectedThreads)
                        connectedThread.write(msgBytes)
                }
                BT_SERVER_DO_CAPTURE -> {
                    val msgBytes = Messages.build(Messages.MessageType.SERVER_DO_CAPTURE, null)

                    for(connectedThread in connectedThreads)
                        connectedThread.write(msgBytes)
                }
            }
        }
    }

    private var handler: Handler = Handler(Handler.Callback { msg ->
        val msgType = msg.what
        val response = msg.obj as ConnectedThread.ResponseMessage

        when(msgType) {
            Messages.MessageType.CLIENT_TIMELAPSE_INITIALIZED.ordinal -> {
                clientsReadyForCapture++
                Util.log("BluetoothServer CLIENT_TIMELAPSE_INITIALIZED $clientsReadyForCapture / ${connectedThreads.size}")
                if(clientsReadyForCapture == connectedThreads.size) {
                    Util.broadcastMessage(applicationContext, BT_SERVER_CLIENTS_READY)
                }
            }
        }
        true
    })

    override fun onCreate() {
        super.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        LocalBroadcastManager.getInstance(applicationContext).registerReceiver(mMessageReceiver, IntentFilter(MainActivity.BROADCAST_FILTER))

        Util.log("BluetoothServerService::onStartCommand!")
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
                    break
                }

                (clients as ArrayList).add(socket)
                manageSocket(socket)
            }
        }

        worker = WorkerThread("BluetoothServiceServerThread")
        worker!!.start()
        worker!!.waitUntilReady()
        worker!!.handler!!.post(runnable)

        return START_STICKY
    }

    private var clients: List<BluetoothSocket> = ArrayList()

    fun manageSocket(socket: BluetoothSocket) {
        Util.log("[BluetoothServerService] Some client has connected, initializing input/output streams..")
        val connectedThread = ConnectedThread(socket, this.handler)
        connectedThread.start()

        connectedThreads.add(connectedThread)

//        val msg = MessageCaptureProgress()
//        msg.battery = 77.43f
//        msg.image = byteArrayOf(1, 2, 30, 40, 127, 125)
//
//        val encodedMsg = msg.toByteArray()
//
//        val transport = MessageTransport()
//        transport.id = 150_000_000
//        transport.length = encodedMsg.size
//        transport.data = encodedMsg
//
//        Util.log("Sending msg length ${transport.length}")
//
//        val finalEncodedMsg = transport.toByteArray()
//        connectedThread!!.write(finalEncodedMsg)
    }

    override fun onDestroy() {
        for(connectedThread in connectedThreads) {
            connectedThread.cancel()
        }

        LocalBroadcastManager.getInstance(applicationContext).unregisterReceiver(mMessageReceiver)
    }

    override fun onLowMemory() {
        super.onLowMemory()
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }
}