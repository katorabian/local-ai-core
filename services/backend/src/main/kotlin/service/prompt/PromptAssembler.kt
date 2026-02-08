package com.katorabian.service.prompt

import com.katorabian.domain.ChatMessage
import com.katorabian.domain.ChatSession
import com.katorabian.domain.enum.Role
import com.katorabian.prompt.PromptConfigFactory

class PromptAssembler(
    private val promptConfigFactory: PromptConfigFactory
) {

    fun assemblePrompt(
        session: ChatSession,
        history: List<ChatMessage>,
        taskHints: List<String> = emptyList()
    ): String {
        val system = promptConfigFactory
            .build(session.behaviorPreset, taskHints)
            .render()

        val conversation = history
            .filter { it.role == Role.USER || it.role == Role.ASSISTANT }
            .sortedBy { it.createdAt }
            .joinToString("\n\n") {
                "[${it.role.name.lowercase().replaceFirstChar(Char::uppercase)}]\n${it.content}"
            }

        return buildString {
            appendLine(system)
            appendLine()

            if (conversation.isNotBlank()) {
                appendLine(conversation)
                appendLine()
            }

            appendLine("[Assistant]")
        }
    }
}
