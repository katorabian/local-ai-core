package com.katorabian.llm

import com.katorabian.domain.ChatMessage

interface LlmClient {
    suspend fun generate(
        model: String,
        messages: List<ChatMessage>
    ): String

    suspend fun stream(
        model: String,
        messages: List<ChatMessage>,
        onToken: suspend (String) -> Unit
    )
}
