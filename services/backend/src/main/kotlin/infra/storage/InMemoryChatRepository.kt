package com.katorabian.infra.storage

import com.katorabian.core.repository.ChatRepository
import com.katorabian.domain.ChatMessage
import com.katorabian.domain.ChatSession
import com.katorabian.domain.enum.Role
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class InMemoryChatRepository : ChatRepository {

    private val sessions = ConcurrentHashMap<UUID, ChatSession>()
    private val messages = ConcurrentHashMap<UUID, MutableList<ChatMessage>>()

    override fun createSession(session: ChatSession) {
        sessions[session.id] = session
        messages[session.id] = mutableListOf()
    }

    override fun getSession(id: UUID): ChatSession? = sessions[id]

    override fun getAllSessions(): List<ChatSession> =
        sessions.values.sortedBy { it.createdAt }

    override fun updateSession(session: ChatSession) {
        require(sessions.containsKey(session.id)) {
            "Session not found"
        }
        sessions[session.id] = session
    }

    override fun addMessage(message: ChatMessage) {
        require(message.role != Role.SYSTEM) {
            "SYSTEM messages must not be stored"
        }
        messages[message.sessionId]?.add(message)
    }

    override fun getMessages(sessionId: UUID): List<ChatMessage> =
        messages[sessionId]?.toList() ?: emptyList()
}