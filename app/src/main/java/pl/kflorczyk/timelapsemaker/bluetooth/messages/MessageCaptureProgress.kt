package pl.kflorczyk.timelapsemaker.bluetooth.messages

import java.nio.ByteBuffer

/**
 * Created by Kamil on 2017-12-26.
 */
class MessageCaptureProgress : MessageSerializable {
    override fun toByteArray(): ByteArray {
        val capacity = 4 + image.size

        val buff = ByteBuffer.allocate(capacity)
        buff.putFloat(battery)
        buff.put(image)
        return buff.array()
    }

    var battery: Float = 0f
    var image: ByteArray = byteArrayOf()

    constructor()

    constructor(bytes: ByteArray) {
        val buff = ByteBuffer.wrap(bytes)
        this.battery = buff.float

        var remaining = buff.remaining()
        this.image = ByteArray(remaining)

        var i = 0
        while(remaining-- > 0) {
            this.image[i++] = buff.get()
        }
    }
}