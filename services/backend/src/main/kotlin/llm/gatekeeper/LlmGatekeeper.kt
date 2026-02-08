package com.katorabian.llm.gatekeeper

import com.katorabian.domain.Constants.EMPTY_STRING
import com.katorabian.domain.Constants.ENUM_JOIN_SEPARATOR
import com.katorabian.domain.Constants.LINE_SEPARATOR
import com.katorabian.llm.LlmClient
import com.katorabian.prompt.BehaviorPrompt
import com.katorabian.service.gatekeeper.ExecutionTarget
import com.katorabian.service.gatekeeper.Gatekeeper
import com.katorabian.service.gatekeeper.GatekeeperDecision
import com.katorabian.service.input.CommandSpec
import com.katorabian.service.input.ParsedCommand
import com.katorabian.service.input.UserIntent
import com.katorabian.service.input.UserIntentSpec
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
            Json.decodeFromString(GatekeeperOutput.serializer(), text)
        }.getOrElse {
            return fallback(text)
        }
        val runtimeIntent = when (gatekeeperOutput.intent) {
            UserIntent.Chat::class.java.simpleName -> UserIntent.Chat
            UserIntent.Code::class.java.simpleName -> UserIntent.Code

            UserIntent.Command::class.java.simpleName -> UserIntent.Command(
                name = gatekeeperOutput.command?.name ?: EMPTY_STRING
            )

            UserIntent.ChangeStyle::class.java.simpleName -> {
                val presetName = gatekeeperOutput.command?.args?.firstOrNull()
                    ?: return fallback("missing_style")

                val preset = runCatching {
                    BehaviorPrompt.Preset.valueOf(presetName.uppercase())
                }.getOrElse {
                    return fallback("invalid_style:$presetName")
                }

                UserIntent.ChangeStyle(preset)
            }

            else -> return fallback("unknown_intent:${gatekeeperOutput.intent}")
        }


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

    private fun buildPrompt(input: String): String {

        val executionTargets = ExecutionTarget.entries
            .joinToString(ENUM_JOIN_SEPARATOR) { it.name }

        val intents = UserIntentSpec.allWireNames()
            .joinToString(ENUM_JOIN_SEPARATOR)
        val intentDescriptions = UserIntentSpec.entries
            .joinToString(LINE_SEPARATOR) { "- ${it.wireName}: ${it.description}" }


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
Ты — интерпретатор пользовательского ввода.
Твоя задача — классификация, не генерация.

Верни ОДИН JSON-объект. Без пояснений. Без текста вне JSON.

Формат ответа:

{
  "executionTarget": "$executionTargets",
  "intent": "$intents",
  "command": {
    "name": String,
    "args": [String]
  } | null
}

Допустимые intent'ы:
$intentDescriptions

Допустимые команды:
$commandSpecs

Правила:
1. command != null ТОЛЬКО если intent == Command
2. Используй ТОЛЬКО команды и аргументы из списка выше
3. Запросы про код, программирование, API, UI, Android → intent = Code
4. Если команда не подходит — intent = Chat и command = null
5. Ответ должен начинаться с '{' и заканчиваться '}'
6. Никакого markdown, комментариев или пояснений

Ввод пользователя:
$input

{
""".trimIndent()
    }
}

@Serializable
private data class GatekeeperOutput(
    val executionTarget: ExecutionTarget,
    val intent: String,
    val command: ParsedCommand? = null
)
