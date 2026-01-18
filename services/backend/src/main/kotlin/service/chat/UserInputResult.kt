package com.katorabian.service.chat

sealed interface UserInputResult {

    suspend fun <T> fold(
        onSystemResponse: suspend (String) -> T,
        onForwardToLlm: suspend (String) -> T
    ): T

    data class CommandHandled(val systemResponse: String) : UserInputResult {
        override suspend fun <T> fold(
            onSystemResponse: suspend (String) -> T,
            onForwardToLlm: suspend (String) -> T
        ): T = onSystemResponse(systemResponse)
    }

    data class IntentHandled(val systemResponse: String) : UserInputResult {
        override suspend fun <T> fold(
            onSystemResponse: suspend (String) -> T,
            onForwardToLlm: suspend (String) -> T
        ): T = onSystemResponse(systemResponse)
    }

    data class ForwardToLlm(val userMessage: String) : UserInputResult {
        override suspend fun <T> fold(
            onSystemResponse: suspend (String) -> T,
            onForwardToLlm: suspend (String) -> T
        ): T = onForwardToLlm(userMessage)
    }
}
