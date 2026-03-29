package com.katorabian.core.chat

import com.katorabian.core.model.Model
import com.katorabian.domain.ChatMessage
import com.katorabian.domain.ChatSession
import com.katorabian.domain.chat.ChatEvent
import com.katorabian.service.chat.UserInputResult
import com.katorabian.service.gatekeeper.Gatekeeper
import com.katorabian.service.input.UserInputProcessor
import com.katorabian.service.message.ChatMessageService
import com.katorabian.service.prompt.PromptService
import kotlinx.coroutines.withTimeout

class ChatEngine(
    private val chatModel: Model,
    private val gatekeeper: Gatekeeper,
    private val messageService: ChatMessageService,
    private val promptService: PromptService,
    private val inputProcessor: UserInputProcessor
) {

    suspend fun processMessage(
        session: ChatSession,
        userQuery: String
    ): ChatMessage {

        val decision = gatekeeper.interpret(userQuery)

        return inputProcessor.process(
            session = session,
            input = userQuery,
            decision = decision
        ).fold(
            onSystemResponse = { text ->
                messageService.addAssistantMessage(session.id, text)
            },
            onForwardToLlm = { userMessage ->

                messageService.addUserMessage(session.id, userMessage)

                val messages = promptService.buildPromptForStream(session)

                val response = chatModel.generate(messages)

                messageService.addAssistantMessage(session.id, response)
            }
        )
    }

    suspend fun streamMessage(
        session: ChatSession,
        userQuery: String,
        emit: suspend (ChatEvent) -> Unit
    ) {

        emit(ChatEvent.Thinking())

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

                val messages = promptService.buildPromptForStream(session)

                val buffer = StringBuilder()

                runCatching {
                    withTimeout(120_000) {
                        chatModel.stream(messages) { chunk ->
                            buffer.append(chunk)
                            emit(ChatEvent.Token(chunk))
                        }
                    }

                    emit(ChatEvent.Completed)

                    messageService.addAssistantMessage(
                        session.id,
                        buffer.toString()
                    )

                }.onFailure {
                    emit(ChatEvent.Error(it.message ?: "error"))
                }
            }
        )
    }
}