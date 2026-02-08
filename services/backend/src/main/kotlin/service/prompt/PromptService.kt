package com.katorabian.service.prompt

import com.katorabian.domain.ChatMessage
import com.katorabian.domain.ChatSession
import com.katorabian.storage.ChatSessionStore

class PromptService(
    private val store: ChatSessionStore,
    private val assembler: PromptAssembler
) {


    fun buildPromptForStream(
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
