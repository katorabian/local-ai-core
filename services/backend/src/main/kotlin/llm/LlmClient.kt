package com.katorabian.llm

interface LlmClient {

    // Chat
    suspend fun generate(
        model: String,
        prompt: String
    ): String

    suspend fun stream(
        model: String,
        prompt: String,
        onToken: suspend (String) -> Unit
    )


    //Gatekeeper
    suspend fun generateCompletion(
        model: String,
        prompt: String,
        maxTokens: Int = 64
    ): String
}
