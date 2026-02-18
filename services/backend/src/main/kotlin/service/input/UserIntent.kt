package com.katorabian.service.input

import com.katorabian.domain.Constants.EMPTY_STRING
import com.katorabian.prompt.BehaviorPrompt
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.elementNames

@Serializable
sealed interface UserIntent {
    val wireName: String

    @Serializable
    @SerialName(Chat.WIRE)
    object Chat : UserIntent {
        const val WIRE = "Chat"
        override val wireName = WIRE
    }

    @Serializable
    @SerialName(Code.WIRE)
    object Code : UserIntent {
        const val WIRE = "Code"
        override val wireName = WIRE
    }

    @Serializable
    @SerialName(ChangeStyle.WIRE)
    data class ChangeStyle(
        val preset: BehaviorPrompt.Preset
    ) : UserIntent {
        companion object {
            const val WIRE = "ChangeStyle"
        }
        override val wireName = WIRE
    }

    @Serializable
    @SerialName(Command.WIRE)
    data class Command(
        val name: String,
        val args: List<String> = emptyList()
    ) : UserIntent {
        companion object {
            const val WIRE = "Command"
        }
        override val wireName = WIRE
    }

    companion object {

        val supportedVariants = listOf(
            Chat.WIRE,
            Code.WIRE,
            ChangeStyle.WIRE,
            Command.WIRE
        )

        val intentDescriptions = """
- Chat: Обычный диалог
- Code: Запрос, связанный с программированием
- ChangeStyle: Изменение стиля ответов
- Command: Явная команда, начинающаяся с '/'
""".trimIndent()

        @OptIn(ExperimentalSerializationApi::class)
        fun fromWireName(name: String, command: ParsedCommand?): UserIntent? {
            return when (name) {
                Chat.WIRE -> Chat

                Code.WIRE -> Code

                Command.WIRE -> {
                    val cmdName = command?.name ?: EMPTY_STRING
                    Command(cmdName, command?.args?: emptyList())
                }

                ChangeStyle.WIRE -> {
                    val presetName = command?.args?.firstOrNull()?: return null

                    val preset = runCatching {
                        BehaviorPrompt.Preset.valueOf(presetName.uppercase())
                    }.getOrNull()?: return null

                    ChangeStyle(preset)
                }

                else -> null
            }
        }
    }
}