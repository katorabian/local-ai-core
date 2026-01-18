package com.katorabian.storage

import com.katorabian.domain.ChatMessage
import com.katorabian.domain.ChatSession
import com.katorabian.domain.enum.Role
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

    fun updateSession(session: ChatSession) {
        require(sessions.containsKey(session.id)) {
            "Session not found"
        }
        sessions[session.id] = session
    }

    fun addMessage(message: ChatMessage) {
        require(message.role != Role.SYSTEM) {
            "SYSTEM messages must not be stored in session history"
        }
        messages[message.sessionId]?.add(message)
    }

    fun getMessages(sessionId: UUID): List<ChatMessage> =
        messages[sessionId]?.toList() ?: emptyList()

    fun getAllSessions(): List<ChatSession> =
        sessions.values.sortedBy { it.createdAt }
}
