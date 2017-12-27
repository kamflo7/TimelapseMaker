package pl.kflorczyk.timelapsemaker.bluetooth.messages

import kotlin.reflect.KClass

/**
 * Created by Kamil on 2017-12-27.
 */
class Messages {

    companion object {
        val messages = arrayOf(
                MessageDefinition(MessageType.SERVER_START_TIMELAPSE, false),
                MessageDefinition(MessageType.CLIENT_CAPTURE_PROGRESS, true)
        )

        private fun getMessageDefinition(msgType: MessageType): MessageDefinition? {
            return messages.firstOrNull { it.msgType == msgType }
        }

        fun build(msgType: MessageType, extraData: MessageSerializable?): ByteArray {
            val msgTransport = MessageTransport()
            msgTransport.id = msgType.ordinal

            val messageDefinition = getMessageDefinition(msgType)
            if((!messageDefinition!!.extraData && extraData != null) ||
                messageDefinition!!.extraData && extraData == null)
                throw RuntimeException("Incorrect parameters for this type of Message")

            if(extraData != null) {
                val extraBytes = extraData.toByteArray()
                msgTransport.data = extraBytes
                msgTransport.length = extraBytes.size
            }

            return msgTransport.toByteArray()
        }
    }

    data class MessageDefinition(
            val msgType: MessageType,
            val extraData: Boolean
    )

    enum class MessageType {
        SERVER_START_TIMELAPSE,

        CLIENT_CAPTURE_PROGRESS
    }
}