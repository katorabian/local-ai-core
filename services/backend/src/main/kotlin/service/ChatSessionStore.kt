package com.katorabian.service

import com.katorabian.domain.ChatMessage
import com.katorabian.domain.ChatSession
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class ChatSessionStore {

    private val sessions = ConcurrentHashMap<UUID, ChatSession>()
    private val messages = ConcurrentHashMap<UUID, MutableList<ChatMessage>>()

    fun createSession(session: ChatSession) {
        sessions[session.id] = session
        messages[session.id] = mutableListOf()
    }

    fun getSession(id: UUID): ChatSession? = sessions[id]

    fun addMessage(message: ChatMessage) {
        messages[message.sessionId]?.add(message)
    }

    fun getMessages(sessionId: UUID): List<ChatMessage> =
        messages[sessionId]?.toList() ?: emptyList()

    fun getAllSessions(): List<ChatSession> =
        sessions.values.sortedBy { it.createdAt }

}
