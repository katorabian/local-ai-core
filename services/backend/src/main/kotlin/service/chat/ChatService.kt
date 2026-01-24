package com.katorabian.service.chat

import com.katorabian.domain.ChatMessage
import com.katorabian.domain.ChatSession
import com.katorabian.domain.chat.ChatEvent
import com.katorabian.service.input.UserInputProcessor
import com.katorabian.service.message.ChatMessageService
import com.katorabian.service.model.ModelRouter
import com.katorabian.service.model.ModelService
import com.katorabian.service.prompt.PromptService
import com.katorabian.service.session.ChatSessionService
import java.util.*

class ChatService(
    private val sessionService: ChatSessionService,
    private val messageService: ChatMessageService,
    private val promptService: PromptService,
    private val modelRouter: ModelRouter,
    private val modelService: ModelService,
    private val inputProcessor: UserInputProcessor
) {

    fun createSession(): ChatSession = sessionService.create()

    suspend fun sendMessage(
        sessionId: UUID,
        userQuery: String
    ): ChatMessage {

        val session = sessionService.get(sessionId)

        return inputProcessor.process(
            session = session,
            input = userQuery
        ).fold(
            onSystemResponse = { userMessage ->
                messageService.addAssistantMessage(session.id, userMessage)
            },
            onForwardToLlm = { userMessage ->
                messageService.addUserMessage(session.id, userMessage)

                val prompt = promptService.buildPromptForSession(session)
                val model = modelRouter.resolve(
                    input = userQuery,
                    modelService = modelService
                ).also { println("Using model: ${it.id} (${it.role})") }

                val response = modelService.withInference(model) {
                    model.client.generate(
                        model = model.id,
                        messages = prompt
                    )
                }

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
    ) {
        val session = sessionService.get(sessionId)

        inputProcessor.process(
            session = session,
            input = userQuery
        ).fold(
            onSystemResponse = { text ->
                emit(ChatEvent.SystemMessage(text))
            },
            onForwardToLlm = { userMessage ->

                messageService.addUserMessage(session.id, userMessage)

                emit(ChatEvent.Thinking())

                val buffer = StringBuilder()
                val prompt = promptService.buildPromptForSession(session)

                runCatching {
                    val model = modelRouter.resolve(
                        input = userQuery,
                        modelService = modelService
                    ).also { println("Using model: ${it.id} (${it.role})") }
                    modelService.withInference(model) {
                        model.client.stream(
                            model = model.id,
                            messages = prompt
                        ) { token ->
                            buffer.append(token)
                            emit(ChatEvent.Token(token))
                        }
                    }

                    val full = buffer.toString()
                    messageService.addAssistantMessage(session.id, full)
                    emit(ChatEvent.Completed)

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
}
