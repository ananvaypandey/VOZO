package com.voikes.technologies.vozo.mesh

object Wire {
    const val PROTOCOL_VERSION = 1

    val FIELD_SEP: String = 31.toChar().toString()
    val MSG_SEP: String = 30.toChar().toString()

    fun encode(vararg fields: String): String = fields.joinToString(FIELD_SEP)

    fun hello(peerId: String, displayName: String): String =
        encode("hello", PROTOCOL_VERSION.toString(), peerId, displayName)

    fun chat(peerId: String, body: String): String =
        encode("chat", PROTOCOL_VERSION.toString(), peerId, body)

    fun ack(peerId: String, body: String): String =
        encode("ack", PROTOCOL_VERSION.toString(), peerId, body)

    sealed interface Frame {
        data class Hello(val peerId: String, val displayName: String) : Frame
        data class Chat(val peerId: String, val body: String) : Frame
        data class Ack(val peerId: String, val body: String) : Frame
        data class Unknown(val raw: String) : Frame
    }

    fun parse(raw: String): Frame {
        val p = raw.split(FIELD_SEP)
        return when {
            p.size >= 4 && p[0] == "hello" -> Frame.Hello(p[2], p[3])
            p.size >= 4 && p[0] == "chat" -> Frame.Chat(p[2], p[3])
            p.size >= 4 && p[0] == "ack" -> Frame.Ack(p[2], p[3])
            else -> Frame.Unknown(raw)
        }
    }
}
