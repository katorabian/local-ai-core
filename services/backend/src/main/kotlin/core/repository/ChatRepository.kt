package com.katorabian.core.repository

import com.katorabian.domain.ChatMessage
import com.katorabian.domain.ChatSession
import java.util.*

interface ChatRepository {

    // sessions
    fun createSession(session: ChatSession)
    fun getSession(id: UUID): ChatSession?
    fun getAllSessions(): List<ChatSession>
    fun updateSession(session: ChatSession)

    // messages
    fun addMessage(message: ChatMessage)
    fun getMessages(sessionId: UUID): List<ChatMessage>
}