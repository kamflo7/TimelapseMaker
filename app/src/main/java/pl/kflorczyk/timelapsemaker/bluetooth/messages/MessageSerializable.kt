package pl.kflorczyk.timelapsemaker.bluetooth.messages

import java.nio.ByteBuffer

/**
 * Created by Kamil on 2017-12-26.
 */
interface MessageSerializable {
    fun toByteArray(): ByteArray
}