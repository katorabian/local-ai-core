package com.katorabian.service.chat

class CommandParser {

    fun parse(input: String): ParsedCommand? {
        if (!input.startsWith("/")) return null

        val parts = input.trim().split("\\s+".toRegex())
        val name = parts.first().lowercase()
        val args = parts.drop(1)

        return ParsedCommand(
            name = name,
            args = args
        )
    }
}

data class ParsedCommand(
    val name: String,
    val args: List<String>
)
