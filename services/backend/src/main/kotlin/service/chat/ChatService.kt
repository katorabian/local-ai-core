package com.katorabian.service.chat

import com.katorabian.core.model.Model
import com.katorabian.domain.ChatMessage
import com.katorabian.domain.ChatSession
import com.katorabian.domain.Constants.MAX_CHAT_SERVICE_REQUEST_TIMEOUT
import com.katorabian.domain.Constants.MAX_SSE_CHUNK_SIZE
import com.katorabian.domain.UserContext
import com.katorabian.domain.chat.ChatEvent
import com.katorabian.service.gatekeeper.Gatekeeper
import com.katorabian.service.input.UserInputProcessor
import com.katorabian.service.message.ChatMessageService
import com.katorabian.service.orchestration.SessionExecutionManager
import com.katorabian.service.prompt.PromptService
import com.katorabian.service.session.ChatSessionService
import kotlinx.coroutines.withTimeout
import java.util.*

class ChatService(
    private val chatModel: Model,
    private val gatekeeper: Gatekeeper,
    private val sessionService: ChatSessionService,
    private val messageService: ChatMessageService,
    private val promptService: PromptService,
    private val inputProcessor: UserInputProcessor,
    private val executionManager: SessionExecutionManager
) {

    fun createSession(): ChatSession = sessionService.create()

    suspend fun sendMessage(
        sessionId: UUID,
        userQuery: String
    ): ChatMessage = executionManager.execute(
        UserContext.SINGLE_USER_ID
    ) {

        val session = sessionService.get(sessionId)
        val decision = gatekeeper.interpret(userQuery)

        return@execute inputProcessor.process(
            session = session,
            input = userQuery,
            decision = decision
        ).fold(
            onSystemResponse = { userMessage ->
                messageService.addAssistantMessage(session.id, userMessage)
            },
            onForwardToLlm = { userMessage ->

                messageService.addUserMessage(session.id, userMessage)

                val chatMessages = promptService.buildPromptForStream(session)
                val response = chatModel.generate(chatMessages)

                messageService.addAssistantMessage(
                    sessionId = session.id,
                    content = response
                )
            }
        )
    }

    suspend fun streamMessage(
        sessionId: UUID,
        userQuery: String,
        emit: suspend (ChatEvent) -> Unit
    ) = executionManager.execute(
        UserContext.SINGLE_USER_ID
    ) {

        emit(ChatEvent.Thinking())

        val session = sessionService.get(sessionId)
        val decision = gatekeeper.interpret(userQuery)

        inputProcessor.process(
            session = session,
            input = userQuery,
            decision = decision
        ).fold(
            onSystemResponse = { text ->
                emit(ChatEvent.SystemMessage(text))
            },
            onForwardToLlm = { userMessage ->

                messageService.addUserMessage(session.id, userMessage)

                val buffer = StringBuilder()
                val chatMessages = promptService.buildPromptForStream(session)

                runCatching {
                    withTimeout(MAX_CHAT_SERVICE_REQUEST_TIMEOUT) {
                        chatModel.stream(chatMessages) { chunk ->
                            buffer.append(chunk)
                            splitForSse(chunk).forEach { safePart ->
                                emit(ChatEvent.Token(safePart))
                            }
                        }
                    }

                    emit(ChatEvent.Completed)

                    val full = buffer.toString()
                    messageService.addAssistantMessage(session.id, full)

                }.getOrElse {
                    emit(ChatEvent.Error(it.message ?: "Unknown error"))
                }
            }
        )
    }

    fun getAllSessions(): List<ChatSession> =
        sessionService.list()

    fun getSession(sessionId: UUID): ChatSession =
        sessionService.get(sessionId)

    fun getSessionMessages(sessionId: UUID): List<ChatMessage> =
        messageService.getMessages(sessionId)

    private fun splitForSse(text: String): List<String> =
        text.chunked(MAX_SSE_CHUNK_SIZE)
}
