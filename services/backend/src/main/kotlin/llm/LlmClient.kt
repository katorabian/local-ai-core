package com.katorabian.llm

import com.katorabian.domain.ChatMessage

interface LlmClient {

    // Chat
    suspend fun generate(
        model: String,
        messages: List<ChatMessage>,
    ): String

    suspend fun stream(
        model: String,
        messages: List<ChatMessage>,
        onToken: suspend (String) -> Unit
    )


    //Gatekeeper
    suspend fun generateCompletion(
        model: String,
        prompt: String,
        maxTokens: Int = 64
    ): String
}
