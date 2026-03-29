package com.katorabian.core.prompt

import com.katorabian.core.repository.ChatRepository
import com.katorabian.domain.ChatMessage
import com.katorabian.domain.ChatSession
import com.katorabian.service.prompt.PromptAssembler

class PromptBuilder(
    private val repository: ChatRepository,
    private val assembler: PromptAssembler
) {


    fun build(
        session: ChatSession,
        taskHints: List<String> = emptyList()
    ): List<ChatMessage> {
        val history = repository.getMessages(session.id)
        return assembler.assemble(
            session = session,
            history = history,
            taskHints = taskHints
        )
    }
}