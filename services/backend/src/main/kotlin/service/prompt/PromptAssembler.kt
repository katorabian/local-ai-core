package com.katorabian.service.prompt

import com.katorabian.domain.ChatMessage
import com.katorabian.domain.ChatSession
import com.katorabian.domain.enum.Role
import com.katorabian.prompt.PromptConfigFactory
import java.time.Instant
import java.util.UUID

class PromptAssembler(
    private val promptConfigFactory: PromptConfigFactory
) {

    fun assemble(
        session: ChatSession,
        history: List<ChatMessage>,
        taskHints: List<String> = emptyList()
    ): List<ChatMessage> {

        val systemPrompt = promptConfigFactory
            .build(
                behaviorPreset = session.behaviorPreset,
                taskHints = taskHints
            )
            .render()
            .trim()

        val systemMessage = ChatMessage(
            id = UUID.randomUUID(),
            sessionId = session.id,
            role = Role.SYSTEM,
            content = systemPrompt,
            createdAt = Instant.EPOCH
        )

        val conversation = history
            .filter { it.role == Role.USER || it.role == Role.ASSISTANT }
            .sortedBy { it.createdAt }
//            .takeLast(20) TODO?

        return buildList {
            add(systemMessage)
            addAll(conversation)
        }
    }
}
