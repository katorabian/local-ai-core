package com.katorabian.service.chat

import service.chat.UserInputResult
import com.katorabian.domain.ChatSession

class UserInputProcessor(
    private val commandParser: CommandParser,
    private val commandExecutor: CommandExecutor,
    private val intentDetector: ProseIntentDetector,
    private val sessionService: ChatSessionService
) {

    fun process(
        session: ChatSession,
        input: String
    ): UserInputResult {

        // 1. Консольная команда
        commandParser.parse(input)?.let { command ->
            val response = commandExecutor.execute(session, command)
            return UserInputResult.CommandHandled(response)
        }

        // 2. Запрос в просьбе
        intentDetector.detect(input)?.let { intent ->
            return handleIntent(session, intent)
        }

        // 3. Обычное сообщение
        return UserInputResult.ForwardToLlm(input)
    }

    private fun handleIntent(
        session: ChatSession,
        intent: ProseIntent
    ): UserInputResult {

        return when (intent) {
            is ProseIntent.ChangeStyle -> {
                sessionService.updateBehavior(
                    sessionId = session.id,
                    preset = intent.preset
                )

                UserInputResult.IntentHandled(
                    systemResponse =
                        "Хорошо. Теперь я буду отвечать в стиле: ${intent.preset.name.lowercase()}."
                )
            }
        }
    }
}
