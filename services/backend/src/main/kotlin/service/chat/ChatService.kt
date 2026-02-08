package com.katorabian.service.chat

import com.katorabian.domain.ChatMessage
import com.katorabian.domain.ChatSession
import com.katorabian.domain.Constants.MAX_SSE_CHUNK_SIZE
import com.katorabian.domain.chat.ChatEvent
import com.katorabian.llm.LlmClient
import com.katorabian.service.gatekeeper.ExecutionTarget
import com.katorabian.service.gatekeeper.Gatekeeper
import com.katorabian.service.gatekeeper.GatekeeperDecision
import com.katorabian.service.input.UserInputProcessor
import com.katorabian.service.message.ChatMessageService
import com.katorabian.service.model.ModelDescriptor
import com.katorabian.service.model.ModelRouter
import com.katorabian.service.model.ModelService
import com.katorabian.service.prompt.PromptService
import com.katorabian.service.session.ChatSessionService
import kotlinx.coroutines.withTimeout
import java.util.*

class ChatService(
    private val llmClient: LlmClient,
    private val sessionService: ChatSessionService,
    private val messageService: ChatMessageService,
    private val promptService: PromptService,
    private val modelRouter: ModelRouter,
    private val modelService: ModelService,
    private val inputProcessor: UserInputProcessor,
    private val gatekeeper: Gatekeeper
) {

    fun createSession(): ChatSession = sessionService.create()

    suspend fun sendMessage(
        sessionId: UUID,
        userQuery: String
    ): ChatMessage {

        val session = sessionService.get(sessionId)
        val decision = gatekeeper.interpret(userQuery)

        return inputProcessor.process(
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
                val model = defineModel(decision, userQuery, modelService)
                val response = modelService.withInference(model) {
                    llmClient.generate(
                        model = model.id,
                        messages = chatMessages
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
                    val model = defineModel(decision, userQuery, modelService)
                    modelService.withInference(model) {
                        withTimeout(120_000) {
                            llmClient.stream(
                                model = model.id,
                                messages = chatMessages
                            ) { chunk ->
                                buffer.append(chunk)
                                splitForSse(chunk).forEach { safePart ->
                                    emit(ChatEvent.Token(safePart))
                                }
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

    private fun defineModel(
        decision: GatekeeperDecision,
        userQuery: String,
        modelService: ModelService,
    ): ModelDescriptor {

        val model = when (decision.executionTarget) {

            ExecutionTarget.REMOTE -> runCatching {
                modelRouter.resolveRemote(
                    input = userQuery,
                    modelService = modelService
                )
            }.getOrElse {
                modelRouter.resolveLocal(
                    input = userQuery,
                    modelService = modelService
                )
            }

            ExecutionTarget.LOCAL -> {
                modelRouter.resolveLocal(
                    input = userQuery,
                    modelService = modelService
                )
            }
        }

        return model.also {
            println(
                "Using model: ${it.id} (${it.role}) | " +
                        "intent=${decision.intent} | " +
                        "reason=${decision.reason}"
            )
        }
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
