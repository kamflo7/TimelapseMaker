package pl.kflorczyk.timelapsemaker.bluetooth

import android.bluetooth.BluetoothSocket
import android.os.Handler
import android.util.Log
import android.widget.Toast
import pl.kflorczyk.timelapsemaker.Util
import pl.kflorczyk.timelapsemaker.Util.log
import pl.kflorczyk.timelapsemaker.bluetooth.messages.Messages
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer

/**
 * Created by Kamil on 2017-12-24.
 */
class ConnectedThread(socket: BluetoothSocket, handler: Handler?) : Thread("ConnectedThreadBluetooth") {
    private val TAG = "ConnectedThread"

    private val socket: BluetoothSocket = socket
    private var inputStream: InputStream
    private var outputStream: OutputStream
    private var buffer: ByteArray? = null
    private var handler: Handler? = handler

    companion object {
        val MESSAGE_READ: Int = 1
        val MESSAGE_WRITE: Int = 2
    }

    init {
        var tmpIn: InputStream? = null
        var tmpOut: OutputStream? = null

        try {
            tmpIn = socket.inputStream
            tmpOut = socket.outputStream
        } catch(e: IOException) {
            log(TAG, "init() -> InputStream or OutputStream initialization failed")
            e.printStackTrace()
        }

        inputStream = tmpIn!!
        outputStream = tmpOut!!
    }

    override fun run() {
        while(true) {
            try {
                log(TAG, "run() -> while() -> Start reading single message")

                var read = 0
                var headerMsg = ByteArray(8)

                var msgID = 0
                var dataLength: Int
                var data: ByteArray? = null
                var i = 0

                do {
                    val r = inputStream.read()
                    if(r == -1) {
                        log(TAG, "run() -> while() -> inputStream.read() returned -1 -> END OF STREAM")
                        cancel()
                        return
                    }

                    val b = r.toByte()
                    if(b == (-2).toByte()) break
                    read++

                    if(read <= 8) {
                        headerMsg[read-1] = b
                    } else {
                        data!![i++] = b
                    }

                    if(read == 8) {
                        val allocate = ByteBuffer.wrap(headerMsg)

                        msgID = allocate.int
                        dataLength = allocate.int
                        data = ByteArray(dataLength)


                        var msgDef = Messages.MessageType.values().find { v -> v.ordinal == msgID }
                        log(TAG, "run() -> while() -> (read == 8) -> At this step I know that msgID is $msgID ($msgDef) and data length is $dataLength")
                    }

                } while(b != (-2).toByte())
                log(TAG, "run() -> while() -> End reading single message")

                val responseMsg = ResponseMessage(this.socket, data!!)

                val msg = handler?.obtainMessage(msgID, -1, -1, responseMsg)
                msg?.sendToTarget()
            } catch (e: IOException) {
                e.printStackTrace()
                return
            }
        }
    }

    fun write(bytes: ByteArray) {
        log(TAG, "write(ByteArray) // ByteArray.size = ${bytes.size}")
        try {
            outputStream.write(bytes)
        } catch(e: IOException) {
            e.printStackTrace()
            log(TAG,"write(ByteArray) -> IOException")
            handler!!.obtainMessage(Messages.MessageType.DEBUG.ordinal, "ConnectedThread::write EXCEPTION").sendToTarget()
        }
    }

    fun cancel() {
        try {
            socket.close()
        } catch(e: IOException) {
            e.printStackTrace()
        }
    }

    data class ResponseMessage(
            val socket: BluetoothSocket,
            val data: ByteArray
    )
}