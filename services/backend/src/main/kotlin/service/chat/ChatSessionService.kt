package com.katorabian.service.chat

import com.katorabian.domain.ChatSession
import com.katorabian.prompt.BehaviorPrompt
import com.katorabian.storage.ChatSessionStore
import java.time.Instant
import java.util.UUID

class ChatSessionService(
    private val store: ChatSessionStore
) {

    fun create(model: String): ChatSession {
        val session = ChatSession(
            id = UUID.randomUUID(),
            model = model,
            behaviorPreset = BehaviorPrompt.Preset.NEUTRAL,
            createdAt = Instant.now()
        )
        store.createSession(session)
        return session
    }

    fun get(sessionId: UUID): ChatSession =
        store.getSession(sessionId)
            ?: error("Session not found")

    fun list(): List<ChatSession> =
        store.getAllSessions()

    fun updateBehavior(
        sessionId: UUID,
        preset: BehaviorPrompt.Preset
    ): ChatSession {
        val current = get(sessionId)

        val updated = current.copy(
            behaviorPreset = preset
        )

        store.updateSession(updated)
        return updated
    }

    fun resetBehavior(sessionId: UUID): ChatSession =
        updateBehavior(sessionId, BehaviorPrompt.Preset.NEUTRAL)
}
