package com.katorabian.service.input

import com.katorabian.prompt.BehaviorPrompt

/**
 * Единая точка истины для поддерживаемых команд.
 */
enum class CommandSpec(
    val commandName: String,
    val argsSpec: ArgsSpec
) {

    STYLE(
        commandName = "/style",
        argsSpec = ArgsSpec.EnumArg(
            name = "preset",
            values = BehaviorPrompt.Preset.entries.map { it.name.lowercase() }
        )
    ),

    RESET_STYLE(
        commandName = "/reset-style",
        argsSpec = ArgsSpec.None
    );


    sealed interface ArgsSpec {
        object None : ArgsSpec

        data class EnumArg(
            val name: String,
            val values: List<String>
        ) : ArgsSpec
    }

    companion object {
        fun byName(name: String): CommandSpec? = entries.firstOrNull { it.commandName == name }
    }
}
