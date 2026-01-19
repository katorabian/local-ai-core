package com.katorabian.service.chat

import com.katorabian.domain.ChatMessage
import com.katorabian.domain.ChatSession
import com.katorabian.llm.LlmClient
import com.katorabian.service.input.UserInputProcessor
import com.katorabian.service.message.ChatMessageService
import com.katorabian.service.model.ModelService
import com.katorabian.service.prompt.PromptService
import com.katorabian.service.session.ChatSessionService
import java.util.*

class ChatService(
    private val sessionService: ChatSessionService,
    private val messageService: ChatMessageService,
    private val promptService: PromptService,
    private val llmClient: LlmClient,
    private val modelService: ModelService,
    private val inputProcessor: UserInputProcessor
) {

    fun createSession(model: String): ChatSession =
        sessionService.create(model)

    suspend fun sendMessage(
        sessionId: UUID,
        rawInput: String
    ): ChatMessage {

        val session = sessionService.get(sessionId)

        return inputProcessor.process(
            session = session,
            input = rawInput
        ).fold(
            onSystemResponse = { userMessage ->
                messageService.addAssistantMessage(session.id, userMessage)
            },
            onForwardToLlm = { userMessage ->
                messageService.addUserMessage(session.id, userMessage)

                val prompt = promptService.buildPromptForSession(session)
                val response = modelService.withInference(session.model) {
                    llmClient.generate(
                        model = session.model,
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
        onToken: suspend (String) -> Unit
    ) {
        val session = sessionService.get(sessionId)

        inputProcessor.process(
            session = session,
            input = userQuery
        ).fold(
            onSystemResponse = onToken,
            onForwardToLlm  = { userMessage ->
                messageService.addUserMessage(session.id, userMessage)

                val buffer = StringBuilder()
                val prompt = promptService.buildPromptForSession(session)

                modelService.withInference(session.model) {
                    llmClient.stream(
                        model = session.model,
                        messages = prompt
                    ) { token ->
                        buffer.append(token)
                        onToken(token)
                    }
                }

                messageService.addAssistantMessage(
                    sessionId = session.id,
                    content = buffer.toString()
                )
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
