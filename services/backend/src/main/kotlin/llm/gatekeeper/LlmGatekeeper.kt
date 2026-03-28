package com.katorabian.llm.gatekeeper

import com.katorabian.core.model.Model
import com.katorabian.domain.ChatMessage
import com.katorabian.domain.Constants.EMPTY_STRING
import com.katorabian.domain.Constants.ENUM_JOIN_SEPARATOR
import com.katorabian.domain.Constants.LINE_SEPARATOR
import com.katorabian.domain.Constants.ONE
import com.katorabian.domain.Constants.ZERO
import com.katorabian.llm.LlmClient
import com.katorabian.prompt.BehaviorPrompt
import com.katorabian.service.gatekeeper.ExecutionTarget
import com.katorabian.service.gatekeeper.Gatekeeper
import com.katorabian.service.gatekeeper.GatekeeperDecision
import com.katorabian.service.input.CommandSpec
import com.katorabian.service.input.ParsedCommand
import com.katorabian.service.input.UserIntent
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class LlmGatekeeper(
    private val model: Model
) : Gatekeeper {

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Прогрев модели.
     * Вызывается один раз после старта приложения.
     */
    suspend fun warmUp() {
        runCatching {
            model.generate(
                messages = listOf(
                    ChatMessage.system(
                        buildPrompt("Classify: hello")
                    )
                )
            )
        }
    }

    override suspend fun interpret(input: String): GatekeeperDecision {
        val rawResponse = runCatching {
            withTimeout(WARM_UP_TIMEOUT) {
                model.generate(
                    messages = listOf(
                        ChatMessage.system(buildPrompt(input))
                    )
                )
            }
        }.getOrElse { ex ->
            return fallback("timeout:${ex::class.simpleName}")
        }

        val text = extractTextFromCompletion(rawResponse)
            ?: return fallback("no_text")

        println("Gatekeeper rawResponse='$rawResponse'")
        println("Gatekeeper extracted text='$text'")

        val gatekeeperOutput = runCatching {
            json.decodeFromString(GatekeeperOutput.serializer(), text)
        }.getOrElse {
            return fallback("invalid_json: $text")
        }
        val runtimeIntent = UserIntent.fromWireName(
            gatekeeperOutput.intent,
            gatekeeperOutput.command
        ) ?: return fallback("unknown_intent:${gatekeeperOutput.intent}")


        return GatekeeperDecision(
            executionTarget = gatekeeperOutput.executionTarget,
            intent = runtimeIntent,
            command = gatekeeperOutput.command,
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
        val start = indexOf('{')
        val end = lastIndexOf('}')
        if (start < ZERO || end < ZERO || end <= start) return this
        return substring(start, end + ONE)
    }

    private fun fallback(raw: String) =
        GatekeeperDecision(
            executionTarget = ExecutionTarget.LOCAL,
            intent = UserIntent.Chat,
            command = null,
            reason = "invalid_output:$raw"
        )

    private fun buildPrompt(input: String): String {

        val executionTargets = ExecutionTarget.entries
            .joinToString(ENUM_JOIN_SEPARATOR) { it.name }

        val intents = UserIntent.supportedVariants
            .joinToString(ENUM_JOIN_SEPARATOR)
        val intentDescriptions = UserIntent.intentDescriptions

        val commandSpecs = CommandSpec.entries
            .joinToString(LINE_SEPARATOR) { spec ->
                when (val args = spec.argsSpec) {
                    CommandSpec.ArgsSpec.None ->
                        "- ${spec.commandName} (no arguments)"
                    is CommandSpec.ArgsSpec.EnumArg ->
                        "- ${spec.commandName} <${args.name}>: ${args.values.joinToString(ENUM_JOIN_SEPARATOR)}"
                }
            }

        return """
You classify user input.

Return ONLY JSON.

Schema:

{
 "executionTarget": "$executionTargets",
 "intent": "$intents",
 "command": { "name": String, "args": [String] } | null
}

Rules:
$intentDescriptions

Commands:
$commandSpecs

User input:
$input

JSON:
""".trimIndent()
    }

    companion object {
        private const val WARM_UP_TIMEOUT = 8_000L
    }
}

@Serializable
private data class GatekeeperOutput(
    val executionTarget: ExecutionTarget,
    val intent: String,
    val command: ParsedCommand? = null
)
