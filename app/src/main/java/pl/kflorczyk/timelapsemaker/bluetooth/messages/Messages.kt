package pl.kflorczyk.timelapsemaker.bluetooth.messages

/**
 * Created by Kamil on 2017-12-27.
 */
class Messages {
    enum class MessageType {
        SERVER_START_TIMELAPSE,
        SERVER_DO_CAPTURE,

        CLIENT_TIMELAPSE_INITIALIZED,
        CLIENT_CAPTURED,
        //CLIENT_CAPTURE_PROGRESS,
        //CLIENT_MESSAGE_CAPTURE // optional extra MessageCaptureWithImage
        DEBUG
    }

    companion object {
//        val messages = arrayOf(
//                MessageDefinition(MessageType.SERVER_START_TIMELAPSE),
//                MessageDefinition(MessageType.SERVER_DO_CAPTURE),
//                MessageDefinition(MessageType.CLIENT_TIMELAPSE_INITIALIZED),
//                MessageDefinition(MessageType.CLIENT_CAPTURE_PROGRESS, true)
//        )

//        private fun getMessageDefinition(msgType: MessageType): MessageDefinition? {
//            return messages.firstOrNull { it.msgType == msgType }
//        }

        fun build(msgType: MessageType, extraData: MessageSerializable? = null): ByteArray {
            val msgTransport = MessageTransport()
            msgTransport.id = msgType.ordinal

//            val messageDefinition = getMessageDefinition(msgType)
//            if((!messageDefinition!!.extraData && extraData != null) ||
//                messageDefinition!!.extraData && extraData == null)
//                throw RuntimeException("Incorrect parameters for this type of Message")

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
            val extraData: Boolean = false
    )
}