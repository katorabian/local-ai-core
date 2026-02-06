package com.katorabian.llm.gatekeeper

import com.katorabian.llm.LlmClient
import com.katorabian.service.gatekeeper.ExecutionTarget
import com.katorabian.service.gatekeeper.Gatekeeper
import com.katorabian.service.gatekeeper.GatekeeperDecision
import com.katorabian.service.input.ParsedCommand
import com.katorabian.service.input.UserIntent
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class LlmGatekeeper(
    private val descriptor: GatekeeperDescriptor,
    private val llmClient: LlmClient
) : Gatekeeper {

    override suspend fun interpret(input: String): GatekeeperDecision {

        val rawResponse = runCatching {
            withTimeout(30_000) {
                llmClient.generateCompletion(
                    model = descriptor.id,
                    prompt = buildPrompt(input),
                    maxTokens = 64
                )
            }
        }.getOrElse { ex ->
            return fallback("timeout:${ex::class.simpleName}")
        }

        val text = extractTextFromCompletion(rawResponse)
            ?: return fallback("no_text")

        println("Gatekeeper rawResponse='$rawResponse'")
        println("Gatekeeper extracted text='$text'")

        val parsed = runCatching {
            Json.decodeFromString(GatekeeperOutput.serializer(), text)
        }.getOrElse {
            return fallback(text)
        }

        return GatekeeperDecision(
            executionTarget = parsed.executionTarget,
            intent = parsed.intent,
            command = parsed.command,
            reason = "gatekeeper"
        )
    }



    private fun extractTextFromCompletion(raw: String): String? =
        runCatching {
            val root = Json.parseToJsonElement(raw).jsonObject
            root["choices"]
                ?.jsonArray
                ?.firstOrNull()
                ?.jsonObject
                ?.get("text")
                ?.jsonPrimitive
                ?.content
                ?.extractJson()
        }.onFailure {
            println("extractTextFromCompletion: $it")
        }.getOrNull()

    private fun String.extractJson(): String {
        return substringAfter("{")
            .substringBeforeLast("}")
            .let { "{$it}" }
    }

    private fun fallback(raw: String) =
        GatekeeperDecision(
            executionTarget = ExecutionTarget.LOCAL,
            intent = UserIntent.Chat,
            command = null,
            reason = "invalid_output:$raw"
        )

    private fun buildPrompt(input: String): String =
        """
        Ты — интерпретатор пользовательского ввода.

        Верни ТОЛЬКО валидный JSON строго по схеме:

        {
          "executionTarget": "LOCAL | REMOTE",
          "intent": "CHAT | CODE | CHANGE_STYLE | COMMAND",
          "command": {
            "name": "/style",
            "args": ["sarcastic"]
          } | null
        }

        Правила:
        - личное, эмоции, RP → LOCAL
        - код, архитектура, reasoning → REMOTE
        - если команда начинается с "/" → intent=COMMAND
        - если пользователь просит изменить стиль → CHANGE_STYLE
        - без пояснений
        - без markdown
        - без лишних полей
        - ответ должен быть ОДНИМ JSON-объектом
        - не продолжай текст после }
        - не размышляй

        Запрос:
        $input
        
        {
        """.trimIndent()
}

@Serializable
private data class GatekeeperOutput(
    val executionTarget: ExecutionTarget,
    val intent: UserIntent,
    val command: ParsedCommand? = null
)
