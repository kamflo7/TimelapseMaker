package pl.kflorczyk.timelapsemaker.bluetooth

import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import pl.kflorczyk.timelapsemaker.Util
import pl.kflorczyk.timelapsemaker.WorkerThread
import pl.kflorczyk.timelapsemaker.bluetooth.messages.MessageCaptureProgress
import pl.kflorczyk.timelapsemaker.bluetooth.messages.MessageTransport
import java.io.IOException
import kotlin.collections.ArrayList

/**
 * Created by Kamil on 2017-12-20.
 */
class BluetoothServerService : Service() {

    private var worker: WorkerThread? = null
    private var connectedThread: ConnectedThread? = null

    private var handler: Handler = Handler(Handler.Callback { msg ->
        val msgType = msg.what
        val data = msg.obj

        when(msgType) {
            ConnectedThread.MESSAGE_READ -> {
                Util.log("BTServerService.Handler.onMessage type: $msgType, data: $data")
                Util.broadcastMessage(applicationContext, "server_is_getting_msg_from_server")
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
        connectedThread = ConnectedThread(socket, this.handler)
        connectedThread!!.start()

        val msg = MessageCaptureProgress()
        msg.battery = 77.43f
        msg.image = byteArrayOf(1, 2, 30, 40, 127, 125)

        val encodedMsg = msg.toByteArray()

        val transport = MessageTransport()
        transport.id = 150_000_000
        transport.length = encodedMsg.size
        transport.data = encodedMsg

        Util.log("Sending msg length ${transport.length}")

        val finalEncodedMsg = transport.toByteArray()

        connectedThread!!.write(finalEncodedMsg)

        // serialization test
//        val testMsg = TestMessage()
//        testMsg.description = "Przykladowy opis"
//        testMsg.number = 170
//        testMsg.floatNumber = 54.75f
//
//        val bos = ByteArrayOutputStream()
//        ObjectOutputStream(bos).use({ os -> os.writeObject(testMsg) })
//
//        val serialized = bos.toByteArray()
//        connectedThread!!.write(serialized)
        // serialisation END

//        connectedThread!!.write(byteArrayOf(1, 2, 3, 4, 5, 6, 7, -2, (-2).toByte()))
    }

    override fun onDestroy() {
        connectedThread?.cancel()
        connectedThread = null
    }

    override fun onLowMemory() {
        super.onLowMemory()
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }
}