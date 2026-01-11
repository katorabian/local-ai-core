package com.katorabian.service

import com.katorabian.domain.ChatMessage
import com.katorabian.domain.ChatSession
import com.katorabian.domain.enum.Role
import com.katorabian.llm.LlmClient
import java.time.Instant
import java.util.UUID

class ChatService(
    private val llmClient: LlmClient
) {

    fun createSession(model: String): ChatSession {
        return ChatSession(
            id = UUID.randomUUID(),
            model = model,
            createdAt = Instant.now()
        )
    }

    suspend fun sendMessage(
        session: ChatSession,
        userMessage: String
    ): ChatMessage {
        val messages = listOf(
            ChatMessage(
                id = UUID.randomUUID(),
                sessionId = session.id,
                role = Role.USER,
                content = userMessage,
                createdAt = Instant.now()
            )
        )

        val response = llmClient.generate(
            model = session.model,
            messages = messages
        )

        return ChatMessage(
            id = UUID.randomUUID(),
            sessionId = session.id,
            role = Role.ASSISTANT,
            content = response,
            createdAt = Instant.now()
        )
    }
}
