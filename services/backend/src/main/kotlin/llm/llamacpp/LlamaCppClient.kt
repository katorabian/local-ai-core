package com.katorabian.llm.llamacpp

import com.katorabian.domain.Constants.CONNECT_TIMEOUT_MS
import com.katorabian.domain.Constants.EMPTY_STRING
import com.katorabian.domain.Constants.LINE_SEPARATOR
import com.katorabian.domain.Constants.LLM_READ_BUFFER
import com.katorabian.domain.Constants.MAX_CLIENTS
import com.katorabian.domain.Constants.NOT_FOUND
import com.katorabian.domain.Constants.ONE
import com.katorabian.domain.Constants.ZERO
import com.katorabian.llm.LlmClient
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.utils.io.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.yield
import kotlinx.serialization.json.*

class LlamaCppClient(
    private val serverProcess: LlamaServerProcess,
    private val baseUrl: String = "http://localhost:8081"
) : LlmClient {
    private val streamSemaphore = Semaphore(MAX_CLIENTS)

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
        prompt: String
    ): String {
        serverProcess.ensureAlive()
        waitUntilReady()

        val response = client.post("$baseUrl/v1/completions") {
            contentType(ContentType.Application.Json)
             setBody(
                 CompletionRequest(
                     model = model,
                     prompt = prompt,
                     n_predict = 1024,
                     temperature = 0.7
                 )
            )
        }

        val text = response.bodyAsText()
        val json = Json.parseToJsonElement(text).jsonObject

        return json["choices"]
            ?.jsonArray
            ?.firstOrNull()
            ?.jsonObject
            ?.get("text")
            ?.jsonPrimitive
            ?.content
            ?: EMPTY_STRING
    }

    override suspend fun stream(
        model: String,
        prompt: String,
        onToken: suspend (String) -> Unit
    ) = streamSemaphore.withPermit {
        serverProcess.ensureAlive()
        waitUntilReady()

        val response = client.post("$baseUrl/v1/completions") {
            contentType(ContentType.Application.Json)
            setBody(
                buildJsonObject {
                    put("model", model)
                    put("prompt", prompt)
                    put("stream", true)
                }
            )
        }

        val channel = response.bodyAsChannel()
        val buffer = ByteArray(LLM_READ_BUFFER)
        val sb = StringBuilder()

        while (!channel.isClosedForRead) {
            val read = channel.readAvailable(buffer)

            if (read == NOT_FOUND)
                break
            if (read == ZERO) {
                yield()
                continue
            }

            sb.append(buffer.decodeToString(ZERO, read))

            while (true) {
                val index = sb.indexOf(LINE_SEPARATOR)
                if (index == NOT_FOUND) break

                val line = sb.substring(ZERO, index).trim()
                sb.delete(ZERO, index + ONE)

                if (!line.startsWith("data:")) continue

                val payload = line.removePrefix("data:").trim()
                if (payload == "[DONE]") return

                val obj = runCatching {
                    Json.parseToJsonElement(payload).jsonObject
                }.getOrNull() ?: continue

                obj["error"]?.let {
                    throw RuntimeException(it.toString())
                }

                val choice = obj["choices"]
                    ?.jsonArray
                    ?.firstOrNull()
                    ?.jsonObject
                    ?: continue

                val finishReason = choice["finish_reason"]
                    ?.jsonPrimitive
                    ?.contentOrNull

                if (finishReason != null) return

                val text = choice["text"]?.jsonPrimitive?.contentOrNull
                    ?: choice["delta"]
                        ?.jsonObject
                        ?.get("content")
                        ?.jsonPrimitive
                        ?.contentOrNull

                if (!text.isNullOrEmpty()) {
                    onToken(text)
                }
            }
        }
    }

    suspend fun waitUntilReady() {
        serverProcess.waitUntilReady()
    }

    override suspend fun generateCompletion(
        model: String,
        prompt: String,
        maxTokens: Int
    ): String {
        serverProcess.ensureAlive()
        waitUntilReady()

        val response = client.post("$baseUrl/v1/completions") {
            contentType(ContentType.Application.Json)
            setBody(
                CompletionRequest(
                    model = model,
                    prompt = prompt,
                    n_predict = maxTokens,
                    temperature = 0.0
                )
            )
        }

        val text = response.bodyAsText()
        return runCatching {
            Json.parseToJsonElement(text).toString()
        }.getOrNull()
            ?: text.trim()
    }

}
