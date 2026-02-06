package com.katorabian.llm.llamacpp

import kotlinx.serialization.Serializable

@Serializable
data class ChatRequest(
    val messages: List<ChatMessageDto>,
    val stream: Boolean = false
)

@Serializable
data class ChatMessageDto(
    val role: String,
    val content: String
)

@Serializable
data class ChatResponse(
    val choices: List<Choice>
) {
    @Serializable
    data class Choice(
        val message: Message
    ) {
        @Serializable
        data class Message(
            val content: String
        )
    }
}

@Serializable
data class CompletionRequest(
    val model: String,
    val prompt: String,
    val n_predict: Int = 4,
    val temperature: Double = 0.0
)
