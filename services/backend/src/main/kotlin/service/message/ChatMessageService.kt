package com.katorabian.service.message

import com.katorabian.domain.ChatMessage
import com.katorabian.domain.enum.Role
import com.katorabian.storage.ChatSessionStore
import java.time.Instant
import java.util.UUID

class ChatMessageService(
    private val store: ChatSessionStore
) {

    fun addUserMessage(
        sessionId: UUID,
        content: String
    ): ChatMessage =
        addMessage(
            sessionId = sessionId,
            role = Role.USER,
            content = content
        )

    fun addAssistantMessage(
        sessionId: UUID,
        content: String
    ): ChatMessage =
        addMessage(
            sessionId = sessionId,
            role = Role.ASSISTANT,
            content = content
        )

    fun getMessages(sessionId: UUID): List<ChatMessage> =
        store.getMessages(sessionId)

    private fun addMessage(
        sessionId: UUID,
        role: Role,
        content: String
    ): ChatMessage {
        val message = ChatMessage(
            id = UUID.randomUUID(),
            sessionId = sessionId,
            role = role,
            content = content,
            createdAt = Instant.now()
        )
        store.addMessage(message)
        return message
    }
}