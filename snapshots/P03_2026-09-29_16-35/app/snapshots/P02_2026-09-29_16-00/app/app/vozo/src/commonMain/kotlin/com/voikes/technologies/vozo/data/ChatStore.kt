package com.voikes.technologies.vozo.data

import app.cash.sqldelight.Query
import com.voikes.technologies.vozo.db.Conversation
import com.voikes.technologies.vozo.db.Message
import com.voikes.technologies.vozo.db.VozoDatabase
import kotlin.random.Random

class ChatStore(private val database: VozoDatabase) {

    fun observeConversations(): Query<Conversation> =
        database.vozoDatabaseQueries.selectConversations()

    fun createConversation(title: String): String {
        val id = "c_${Random.nextLong().toString(16)}"
        database.transaction {
            database.vozoDatabaseQueries.insertConversation(
                id = id,
                title = title,
                peer_id = null,
                created_at = SYSTEM_TIME,
                updated_at = SYSTEM_TIME,
            )
        }
        return id
    }

    fun observeMessages(conversationId: String): Query<Message> =
        database.vozoDatabaseQueries.selectMessages(conversationId)

    fun sendMessage(conversationId: String, body: String) {
        database.transaction {
            database.vozoDatabaseQueries.insertMessage(
                conversation_id = conversationId,
                sender = SELF,
                body = body,
                status = SENT,
                created_at = SYSTEM_TIME,
            )
            database.vozoDatabaseQueries.touchConversation(
                updated_at = SYSTEM_TIME,
                last_preview = body,
                id = conversationId,
            )
        }
    }

    private companion object {
        const val SELF = "self"
        const val SENT = "sent"
        val SYSTEM_TIME: Long
            get() = System.currentTimeMillis()
    }
}
