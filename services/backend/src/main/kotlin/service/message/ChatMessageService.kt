package com.katorabian.service.message

import com.katorabian.core.repository.ChatRepository
import com.katorabian.domain.ChatMessage
import com.katorabian.domain.enum.Role
import java.time.Instant
import java.util.*

class ChatMessageService(
    private val repo: ChatRepository
) {

    fun addUserMessage(
        sessionId: UUID,
        content: String
    ): ChatMessage =
        addMessage(sessionId, Role.USER, content)

    fun addAssistantMessage(
        sessionId: UUID,
        content: String
    ): ChatMessage =
        addMessage(sessionId, Role.ASSISTANT, content)

    fun getMessages(sessionId: UUID): List<ChatMessage> =
        repo.getMessages(sessionId)

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
        repo.addMessage(message)
        return message
    }
}