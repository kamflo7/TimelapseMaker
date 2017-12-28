package pl.kflorczyk.timelapsemaker.bluetooth.messages

import java.nio.ByteBuffer

/**
 * Created by Kamil on 2017-12-26.
 */
class MessageCaptureWithImage : MessageSerializable {
    override fun toByteArray(): ByteArray {
        val capacity = image.size

        val buff = ByteBuffer.allocate(capacity)
        buff.put(image)
        return buff.array()
    }

    var image: ByteArray = byteArrayOf()

    constructor()

    constructor(bytes: ByteArray) {
        val buff = ByteBuffer.wrap(bytes)

        var remaining = buff.remaining()
        this.image = ByteArray(remaining)

        var i = 0
        while(remaining-- > 0) {
            this.image[i++] = buff.get()
        }
    }
}