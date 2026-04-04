package com.katorabian.core.prompt

import com.katorabian.domain.ChatMessage
import com.katorabian.domain.ChatSession
import com.katorabian.service.prompt.PromptAssembler

class PromptBuilder(
    private val assembler: PromptAssembler
) {

    fun build(
        session: ChatSession,
        history: List<ChatMessage>,
        taskHints: List<String> = emptyList()
    ): List<ChatMessage> {
        return assembler.assemble(
            session = session,
            history = history,
            taskHints = taskHints
        )
    }
}