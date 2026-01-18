package com.katorabian.service.chat

import com.katorabian.domain.ChatSession
import com.katorabian.prompt.BehaviorPrompt

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

                // TODO: на следующем шаге — корректное обновление session state
                "Style will be changed to ${preset.name.lowercase()} (not yet persisted)"
            }

            "/reset-style" -> {
                "Style reset to neutral (not yet persisted)"
            }

            else -> "Unknown command: ${command.name}"
        }
    }
}
