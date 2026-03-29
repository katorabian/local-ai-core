package com.katorabian.core.prompt

import com.katorabian.domain.ChatMessage
import com.katorabian.domain.ChatSession
import com.katorabian.service.prompt.PromptAssembler
import com.katorabian.storage.ChatSessionStore

class PromptBuilder(
    private val store: ChatSessionStore,
    private val assembler: PromptAssembler
) {


    fun build(
        session: ChatSession,
        taskHints: List<String> = emptyList()
    ): List<ChatMessage> {
        val history = store.getMessages(session.id)
        return assembler.assemble(
            session = session,
            history = history,
            taskHints = taskHints
        )
    }
}