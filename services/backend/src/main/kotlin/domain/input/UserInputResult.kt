package com.katorabian.domain.input

sealed interface UserInputResult {

    suspend fun <T> fold(
        onSystemResponse: suspend (String) -> T,
        onForwardToLlm: suspend (String) -> T
    ): T

    data class SystemMessage(
        val text: String
    ) : UserInputResult {
        override suspend fun <T> fold(
            onSystemResponse: suspend (String) -> T,
            onForwardToLlm: suspend (String) -> T
        ): T = onSystemResponse(text)
    }

    data class ForwardToLlm(
        val userMessage: String
    ) : UserInputResult {
        override suspend fun <T> fold(
            onSystemResponse: suspend (String) -> T,
            onForwardToLlm: suspend (String) -> T
        ): T = onForwardToLlm(userMessage)
    }
}