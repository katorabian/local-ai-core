package com.katorabian.llm.llamacpp

import com.katorabian.domain.ChatMessage
import com.katorabian.llm.LlmClient
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.http.*

class LlamaCppClient(
    private val baseUrl: String = "http://localhost:8081"
) : LlmClient {

    private val client = HttpClient(CIO)

    override suspend fun generate(
        model: String,
        messages: List<ChatMessage>
    ): String {

        val response = client.post("$baseUrl/v1/chat/completions") {
            contentType(ContentType.Application.Json)
            setBody(
                ChatRequest(
                    messages = messages.map {
                        ChatMessageDto(
                            role = it.role.name.lowercase(),
                            content = it.content
                        )
                    }
                )
            )
        }.body<ChatResponse>()

        return response.choices.first().message.content
    }

    override suspend fun stream(
        model: String,
        messages: List<ChatMessage>,
        onToken: suspend (String) -> Unit
    ) {
        // На первом шаге — не реализуем
        // streaming подключим следующим шагом
        val full = generate(model, messages)
        onToken(full)
    }
}
