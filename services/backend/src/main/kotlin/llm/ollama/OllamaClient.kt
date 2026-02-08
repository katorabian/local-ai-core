package com.katorabian.llm.ollama

import com.katorabian.domain.ChatMessage
import com.katorabian.domain.Constants.CONNECT_TIMEOUT_MS
import com.katorabian.domain.Constants.LLM_READ_BUFFER
import com.katorabian.domain.Constants.NOT_FOUND
import com.katorabian.domain.Constants.REQUEST_TIMEOUT_WARMUP_MS
import com.katorabian.domain.Constants.ZERO
import com.katorabian.domain.enum.Role
import com.katorabian.llm.LlmClient
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
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
        val buffer = StringBuilder()
        val readBuffer = ByteArray(LLM_READ_BUFFER)

        while (!channel.isClosedForRead) {
            val read = channel.readAvailable(readBuffer, ZERO, readBuffer.size)
            if (read <= ZERO) continue

            buffer.append(String(readBuffer, ZERO, read, Charsets.UTF_8))

            var newlineIndex: Int
            while (true) {
                newlineIndex = buffer.indexOf("\n")
                    .takeIf { it != NOT_FOUND }
                    ?: break

                val line = buffer.substring(ZERO, newlineIndex).trim()
                buffer.delete(ZERO, newlineIndex + 1)
                if (line.isEmpty()) continue

                val chunk = json.decodeFromString<OllamaResponse>(line)
                if (chunk.response.isNotEmpty()) {
                    onToken(chunk.response)
                }

                if (chunk.done) {
                    return
                }
            }
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

    override suspend fun generateCompletion(
        model: String,
        prompt: String,
        maxTokens: Int
    ): String {
        TODO("Not yet implemented")
    }
}