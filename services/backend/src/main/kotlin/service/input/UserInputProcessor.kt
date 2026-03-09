package com.katorabian.service.input

import com.katorabian.domain.ChatSession
import com.katorabian.service.chat.UserInputResult
import com.katorabian.service.chat.UserInputResult.*
import com.katorabian.service.gatekeeper.GatekeeperDecision
import com.katorabian.service.session.ChatSessionService

class UserInputProcessor(
    private val commandExecutor: CommandExecutor,
    private val sessionService: ChatSessionService
) {

    fun process(
        session: ChatSession,
        input: String,
        decision: GatekeeperDecision
    ): UserInputResult {

        return when (val intent = decision.intent) {

            is UserIntent.Command -> {
                val command = decision.command
                    ?: return SystemMessage(
                        "Команда не распознана."
                    )

                val response = commandExecutor.execute(
                    session = session,
                    command = command
                )

                SystemMessage(response)
            }

            is UserIntent.ChangeStyle -> {
                sessionService.updateBehavior(
                    sessionId = session.id,
                    preset = intent.preset
                )

                SystemMessage(
                    "Хорошо. Теперь я буду отвечать в стиле: ${intent.preset.name.lowercase()}."
                )
            }

            UserIntent.Chat,
            UserIntent.Code,
            UserIntent.Intimate -> {
                ForwardToLlm(input)
            }
        }
    }
}
