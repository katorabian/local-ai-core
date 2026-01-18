package com.katorabian.service.input

import com.katorabian.domain.ChatSession
import com.katorabian.prompt.BehaviorPrompt
import com.katorabian.service.session.ChatSessionService

class CommandExecutor(
    private val sessionService: ChatSessionService
) {

    fun execute(
        session: ChatSession,
        command: ParsedCommand
    ): String {

        return when (command.name) {

            "/style" -> {
                val presetName = command.args.firstOrNull()
                    ?: return "Usage: /style <neutral|sarcastic|formal|concise>"

                val preset = runCatching {
                    BehaviorPrompt.Preset.valueOf(presetName.uppercase())
                }.getOrNull()
                    ?: return "Unknown style: $presetName"

                sessionService.updateBehavior(
                    sessionId = session.id,
                    preset = preset
                )

                "Style changed to ${preset.name.lowercase()}"
            }

            "/reset-style" -> {
                sessionService.resetBehavior(session.id)
                "Style reset to neutral"
            }

            else -> "Unknown command: ${command.name}"
        }
    }
}
