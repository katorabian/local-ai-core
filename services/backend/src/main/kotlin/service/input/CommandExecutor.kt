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

        val spec = CommandSpec.byName(command.name)
            ?: return "Unknown command: ${command.name}"

        return when (spec) {

            CommandSpec.STYLE -> {
                val presetName = command.args.firstOrNull()
                    ?: return "Usage: /style <${spec.argsSpec}>"

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

            CommandSpec.RESET_STYLE -> {
                sessionService.resetBehavior(session.id)
                "Style reset to neutral"
            }
        }
    }
}
