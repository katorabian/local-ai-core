package com.katorabian.llm.ollama

import com.katorabian.domain.ChatMessage
import com.katorabian.domain.enum.Role
import com.katorabian.llm.LlmClient
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.timeout
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

class OllamaClient(
    private val baseUrl: String = "http://localhost:11434"
): LlmClient {

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }

        install(HttpTimeout) {
            connectTimeoutMillis = CONNECT_TIMEOUT_MS
            // requestTimeoutMillis задаём динамически в запросах
            socketTimeoutMillis = HttpTimeout.INFINITE_TIMEOUT_MS
        }
    }

    override suspend fun generate(
        model: String,
        messages: List<ChatMessage>
    ): String {
        val prompt = buildPrompt(messages)

        val response = client.post("$baseUrl/api/generate") {
            contentType(ContentType.Application.Json)
            timeout {
                requestTimeoutMillis = REQUEST_TIMEOUT_WARMUP_MS
            }
            setBody(
                OllamaRequest(
                    model = model,
                    prompt = prompt,
                    stream = false
                )
            )
        }.body<OllamaResponse>()

        return response.response
    }

    override suspend fun stream(
        model: String,
        messages: List<ChatMessage>,
        onToken: suspend (String) -> Unit
    ) {
        val prompt = buildPrompt(messages)

        val channel = client.post("$baseUrl/api/generate") {
            contentType(ContentType.Application.Json)
            timeout {
                requestTimeoutMillis = REQUEST_TIMEOUT_WARMUP_MS
            }
            setBody(
                OllamaRequest(
                    model = model,
                    prompt = prompt,
                    stream = true
                )
            )
        }.bodyAsChannel()

        val json = Json { ignoreUnknownKeys = true }

        while (!channel.isClosedForRead) {
            val line = channel.readUTF8Line(DEFAULT_BUFFER_SIZE)?: continue
            val chunk = json.decodeFromString<OllamaResponse>(line)

            if (chunk.response.isNotEmpty()) {
                onToken(chunk.response)
            }

            if (chunk.done) break
        }
    }

    private fun buildPrompt(messages: List<ChatMessage>): String {
        return messages.joinToString("\n") { msg ->
            when (msg.role) {
                Role.SYSTEM -> "System: ${msg.content}"
                Role.USER -> "User: ${msg.content}"
                Role.ASSISTANT -> "Assistant: ${msg.content}"
            }
        }
    }

    companion object {
        private const val CONNECT_TIMEOUT_MS = 5_000L
        private const val REQUEST_TIMEOUT_READY_MS = 30_000L
        private const val REQUEST_TIMEOUT_WARMUP_MS = 120_000L
    }
}