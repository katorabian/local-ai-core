package com.katorabian.service.input

import com.katorabian.domain.Constants.EMPTY_STRING
import com.katorabian.prompt.BehaviorPrompt
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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

    @Serializable
    @SerialName(Intimate.WIRE)
    object Intimate : UserIntent {
        const val WIRE = "Intimate"
        override val wireName = WIRE
    }

    companion object {

        val supportedVariants = listOf(
            Chat.WIRE,
            Code.WIRE,
            ChangeStyle.WIRE,
            Command.WIRE,
            Intimate.WIRE
        )

        val intentDescriptions = """
- Commands start with '/'
- If command → intent = Command
- Style change → intent = ChangeStyle
- Programming question → intent = Code
- Erotic or emotional experience -> intent = Intimate
- Otherwise → Chat
""".trimIndent()

        @OptIn(ExperimentalSerializationApi::class)
        fun fromWireName(name: String, command: ParsedCommand?): UserIntent? {
            return when (name) {
                Chat.WIRE -> Chat

                Code.WIRE -> Code

                Intimate.WIRE -> Code

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