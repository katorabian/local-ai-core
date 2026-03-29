package com.katorabian.service.chat

import com.katorabian.core.chat.ChatEngine
import com.katorabian.domain.ChatMessage
import com.katorabian.domain.ChatSession
import com.katorabian.domain.UserContext
import com.katorabian.domain.chat.ChatEvent
import com.katorabian.service.orchestration.SessionExecutionManager
import com.katorabian.service.session.ChatSessionService
import java.util.*

class ChatService(
    private val engine: ChatEngine,
    private val sessionService: ChatSessionService,
    private val executionManager: SessionExecutionManager
) {

    fun createSession(): ChatSession = sessionService.create()

    suspend fun sendMessage(
        sessionId: UUID,
        userQuery: String
    ): ChatMessage =
        executionManager.execute(UserContext.SINGLE_USER_ID) {

            val session = sessionService.get(sessionId)

            engine.processMessage(
                session = session,
                userQuery = userQuery
            )
        }

    suspend fun streamMessage(
        sessionId: UUID,
        userQuery: String,
        emit: suspend (ChatEvent) -> Unit
    ) =
        executionManager.execute(UserContext.SINGLE_USER_ID) {

            val session = sessionService.get(sessionId)

            engine.streamMessage(
                session = session,
                userQuery = userQuery,
                emit = emit
            )
        }

    fun getAllSessions(): List<ChatSession> =
        sessionService.list()

    fun getSession(sessionId: UUID): ChatSession =
        sessionService.get(sessionId)

    fun getSessionMessages(sessionId: UUID): List<ChatMessage> =
        sessionService.get(sessionId)
            .let { emptyList() } //TODO
}