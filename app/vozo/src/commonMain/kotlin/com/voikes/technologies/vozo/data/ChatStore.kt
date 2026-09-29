package com.voikes.technologies.vozo.data

import app.cash.sqldelight.Query
import com.voikes.technologies.vozo.db.Conversation
import com.voikes.technologies.vozo.db.Message
import com.voikes.technologies.vozo.db.VozoDatabase
import com.voikes.technologies.vozo.mesh.Transport
import com.voikes.technologies.vozo.mesh.Wire
import kotlin.random.Random

class ChatStore(
    private val database: VozoDatabase,
) {

    var transport: Transport? = null
        private set

    fun attachTransport(t: Transport) {
        transport = t
    }

    val selfId: String by lazy {
        localState(KEY_SELF_ID) ?: "vozo-${Random.nextLong().toString(16)}".also {
            setLocalState(KEY_SELF_ID, it)
        }
    }

    val selfName: String by lazy {
        localState(KEY_SELF_NAME) ?: "VOZO device".also {
            setLocalState(KEY_SELF_NAME, it)
        }
    }

    fun observeConversations(): Query<Conversation> =
        database.vozoDatabaseQueries.selectConversations()

    fun createConversation(title: String): String {
        val id = "c_${Random.nextLong().toString(16)}"
        database.transaction {
            database.vozoDatabaseQueries.insertConversation(
                id = id,
                title = title,
                peer_id = null,
                created_at = now(),
                updated_at = now(),
            )
        }
        return id
    }

    fun observeMessages(conversationId: String): Query<Message> =
        database.vozoDatabaseQueries.selectMessages(conversationId)

    fun sendMessage(conversationId: String, body: String) {
        val trimmed = body.trim()
        if (trimmed.isEmpty()) return

        val peerId = database.vozoDatabaseQueries
            .selectConversationById(conversationId)
            .executeAsOneOrNull()
            ?.peer_id

        database.transaction {
            database.vozoDatabaseQueries.insertMessage(
                conversation_id = conversationId,
                sender = selfId,
                body = trimmed,
                status = if (peerId != null) QUEUED else LOCAL,
                created_at = now(),
            )
            database.vozoDatabaseQueries.touchConversation(
                updated_at = now(),
                last_preview = trimmed,
                id = conversationId,
            )
        }

        if (peerId != null) {
            transport?.send(peerId, Wire.chat(selfId, trimmed))
        }
    }

    /** Handles a frame received from a connected peer. */
    fun onFrame(raw: String) {
        when (val frame = Wire.parse(raw)) {
            is Wire.Frame.Hello -> {
                database.transaction {
                    database.vozoDatabaseQueries.upsertPeer(frame.peerId, frame.displayName, now())
                }
                conversationForPeer(frame.peerId, frame.displayName)
            }

            is Wire.Frame.Chat -> {
                val conversationId = conversationForPeer(frame.peerId)
                database.transaction {
                    database.vozoDatabaseQueries.insertMessage(
                        conversation_id = conversationId,
                        sender = frame.peerId,
                        body = frame.body,
                        status = RECEIVED,
                        created_at = now(),
                    )
                    database.vozoDatabaseQueries.touchConversation(
                        updated_at = now(),
                        last_preview = frame.body,
                        id = conversationId,
                    )
                }
            }

            else -> Unit
        }
    }

    fun conversationForPeer(peerId: String, peerName: String? = null): String {
        database.vozoDatabaseQueries.selectConversationByPeer(peerId).executeAsOneOrNull()
            ?.let { return it.id }

        val id = "p_$peerId"
        val title = peerName?.takeIf { it.isNotBlank() } ?: peerId.takeLast(6).uppercase()
        database.transaction {
            database.vozoDatabaseQueries.insertConversation(
                id = id,
                title = title,
                peer_id = peerId,
                created_at = now(),
                updated_at = now(),
            )
        }
        return id
    }

    fun startPeerChat(peerId: String, peerName: String? = null): String =
        conversationForPeer(peerId, peerName)

    fun localState(key: String): String? =
        database.vozoDatabaseQueries.getLocalState(key).executeAsOneOrNull()

    private fun setLocalState(key: String, value: String) {
        database.vozoDatabaseQueries.setLocalState(key, value)
    }

    private fun now(): Long = System.currentTimeMillis()

    private companion object {
        const val LOCAL = "local"
        const val QUEUED = "queued"
        const val RECEIVED = "received"
        const val KEY_SELF_ID = "self_id"
        const val KEY_SELF_NAME = "self_name"
    }
}
