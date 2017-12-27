package pl.kflorczyk.timelapsemaker.bluetooth

import android.bluetooth.BluetoothSocket
import android.os.Handler
import pl.kflorczyk.timelapsemaker.Util
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer

/**
 * Created by Kamil on 2017-12-24.
 */
class ConnectedThread(socket: BluetoothSocket, handler: Handler?) : Thread("ConnectedThreadBluetooth") {
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
            e.printStackTrace()
        }

        inputStream = tmpIn!!
        outputStream = tmpOut!!
    }

    override fun run() {
        while(true) {
            try {
                Util.log("Start reading single message")

                var read = 0
                var headerMsg = ByteArray(8)

                var msgID = 0
                var dataLength: Int
                var data: ByteArray? = null
                var i = 0

                do {
                    val r = inputStream.read()
                    if(r == -1) {
                        Util.log("ConnectedThread->InputStream::read -1")
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
                        Util.log("At this step I know that msgID is $msgID and data length is $dataLength")
                    }

                } while(b != (-2).toByte())
                Util.log("End reading single message")

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
        Util.log("[ConnectedThread] Probuje wyslac kilka bajtow")
        try {
            outputStream.write(bytes)
        } catch(e: IOException) {
            e.printStackTrace()
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