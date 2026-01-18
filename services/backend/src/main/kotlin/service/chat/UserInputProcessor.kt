package com.katorabian.service.chat

import com.katorabian.domain.ChatSession

class UserInputProcessor(
    private val commandParser: CommandParser,
    private val commandExecutor: CommandExecutor
) {

    fun process(
        session: ChatSession,
        input: String
    ): UserInputResult {

        val command = commandParser.parse(input)
            ?: return UserInputResult.ForwardToLlm(input)

        val response = commandExecutor.execute(session, command)

        return UserInputResult.CommandHandled(
            systemResponse = response
        )
    }
}
