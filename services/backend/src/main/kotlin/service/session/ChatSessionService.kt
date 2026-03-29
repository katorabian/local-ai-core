package com.katorabian.service.session

import com.katorabian.core.repository.ChatRepository
import com.katorabian.domain.ChatSession
import com.katorabian.prompt.BehaviorPrompt
import java.time.Instant
import java.util.*

class ChatSessionService(
    private val repo: ChatRepository
) {

    fun create(): ChatSession {
        val session = ChatSession(
            id = UUID.randomUUID(),
            behaviorPreset = BehaviorPrompt.Preset.NEUTRAL,
            createdAt = Instant.now()
        )
        repo.createSession(session)
        return session
    }

    fun get(sessionId: UUID): ChatSession =
        repo.getSession(sessionId)
            ?: error("Session not found")

    fun list(): List<ChatSession> =
        repo.getAllSessions()

    fun updateBehavior(
        sessionId: UUID,
        preset: BehaviorPrompt.Preset
    ): ChatSession {
        val current = get(sessionId)

        val updated = current.copy(
            behaviorPreset = preset
        )

        repo.updateSession(updated)
        return updated
    }

    fun resetBehavior(sessionId: UUID): ChatSession =
        updateBehavior(sessionId, BehaviorPrompt.Preset.NEUTRAL)
}