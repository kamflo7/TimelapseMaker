package pl.kflorczyk.timelapsemaker.bluetooth.messages

/**
 * Created by Kamil on 2017-12-26.
 */
interface MessageSerializable {
    fun toByteArray(): ByteArray
}