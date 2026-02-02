package com.katorabian.llm.gatekeeper

import com.katorabian.domain.ChatMessage
import com.katorabian.llm.LlmClient
import com.katorabian.service.gatekeeper.ExecutionTarget
import com.katorabian.service.gatekeeper.Gatekeeper
import com.katorabian.service.gatekeeper.GatekeeperDecision
import com.katorabian.service.input.ParsedCommand
import com.katorabian.service.input.UserIntent
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class LlmGatekeeper(
    private val descriptor: GatekeeperDescriptor,
    private val llmClient: LlmClient
) : Gatekeeper {

    override suspend fun interpret(input: String): GatekeeperDecision {

        val raw = runCatching {
            withTimeout(5_000) {
                llmClient.generate(
                    model = descriptor.id,
                    messages = listOf(
                        system(buildPrompt(input))
                    )
                )
            }
        }.getOrElse { ex ->
            return fallback("timeout:${ex::class.simpleName}")
        }

        val parsed = runCatching {
            Json.decodeFromString(GatekeeperOutput.serializer(), raw)
        }.getOrElse {
            return fallback(raw)
        }

        return GatekeeperDecision(
            executionTarget = parsed.executionTarget,
            intent = parsed.intent,
            command = parsed.command,
            reason = "gatekeeper"
        )
    }

    private fun fallback(raw: String) =
        GatekeeperDecision(
            executionTarget = ExecutionTarget.LOCAL,
            intent = UserIntent.Chat,
            command = null,
            reason = "invalid_output:$raw"
        )

    private fun system(text: String) = ChatMessage.system(text)

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
        - не добавляй пробелы или переносы

        Запрос:
        $input
        """.trimIndent()
}

@Serializable
private data class GatekeeperOutput(
    val executionTarget: ExecutionTarget,
    val intent: UserIntent,
    val command: ParsedCommand? = null
)
