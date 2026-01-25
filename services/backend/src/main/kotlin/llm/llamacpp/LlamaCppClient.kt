package com.katorabian.llm.llamacpp

import com.katorabian.domain.ChatMessage
import com.katorabian.domain.Constants.CONNECT_TIMEOUT_MS
import com.katorabian.domain.Constants.LLM_READ_BUFFER
import com.katorabian.domain.Constants.ZERO
import com.katorabian.llm.LlmClient
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.yield
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

class LlamaCppClient(
    private val serverProcess: LlamaServerProcess,
    private val baseUrl: String = "http://localhost:8081"
) : LlmClient {

    init {
        serverProcess.start()
    }

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                    encodeDefaults = true
                }
            )
        }

        install(HttpTimeout) {
            requestTimeoutMillis = HttpTimeout.INFINITE_TIMEOUT_MS
            socketTimeoutMillis = HttpTimeout.INFINITE_TIMEOUT_MS
            connectTimeoutMillis = CONNECT_TIMEOUT_MS
        }
    }

    override suspend fun generate(
        model: String,
        messages: List<ChatMessage>
    ): String {
        serverProcess.ensureAlive()
        waitUntilReady()

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
        serverProcess.ensureAlive()
        waitUntilReady()

        val response = client.post("$baseUrl/v1/chat/completions") {
            contentType(ContentType.Application.Json)
            setBody(
                buildJsonObject {
                    put("model", model)
                    put("stream", true)

                    putJsonArray("messages") {
                        messages.forEach { msg ->
                            add(
                                buildJsonObject {
                                    put("role", msg.role.name.lowercase())
                                    put("content", msg.content)
                                }
                            )
                        }
                    }
                }
            )
        }

        val channel = response.bodyAsChannel()
        val buffer = ByteArray(LLM_READ_BUFFER)

        while (!channel.isClosedForRead) {
            val read = channel.readAvailable(buffer)
            if (read <= ZERO) {
                yield()
                continue
            }

            val chunk = buffer.decodeToString(ZERO, read)

            chunk.lineSequence().forEach { line ->
                if (!line.startsWith("data:")) return@forEach

                val payload = line.removePrefix("data:").trim()
                if (payload == "[DONE]") return

                val obj = runCatching {
                    Json.parseToJsonElement(payload).jsonObject
                }.getOrNull() ?: return@forEach

                val choices = obj["choices"]?.jsonArray ?: return@forEach
                val delta = choices.firstOrNull()
                    ?.jsonObject
                    ?.get("delta")
                    ?.jsonObject

                val content = delta?.get("content")?.jsonPrimitive?.content.takeIf { it != "null" }
                if (!content.isNullOrEmpty()) {
                    onToken(content)
                }
            }
        }
    }

    suspend fun waitUntilReady() {
        serverProcess.waitUntilReady()
    }
}
