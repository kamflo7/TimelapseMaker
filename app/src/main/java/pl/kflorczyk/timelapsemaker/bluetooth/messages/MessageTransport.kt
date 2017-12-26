package pl.kflorczyk.timelapsemaker.bluetooth.messages

import java.io.Serializable
import java.nio.ByteBuffer

/**
 * Created by Kamil on 2017-12-26.
 */
class MessageTransport {
    var id: Int = 0
    var length: Int = 0
    var data: ByteArray = byteArrayOf()

    fun toByteArray(): ByteArray {
        val capacity = 4 + 4 + data.size + 1

        val buff = ByteBuffer.allocate(capacity)
        buff.putInt(id)
        buff.putInt(length)
        buff.put(data)
        buff.put(-2)
        return buff.array()
    }
}