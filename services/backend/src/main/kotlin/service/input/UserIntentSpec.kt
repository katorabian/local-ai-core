package com.katorabian.service.input

/**
 * Каноническое описание поддерживаемых intent'ов
 * для Gatekeeper и валидации.
 */
enum class UserIntentSpec(
    val wireName: String,
    val requiresCommand: Boolean = false,
    val description: String
) {

    CHAT(
        wireName = "Chat",
        description = "Обычный диалог без специальных инструкций"
    ),

    CODE(
        wireName = "Code",
        description = "Запрос, связанный с программированием или кодом"
    ),

    CHANGE_STYLE(
        wireName = "ChangeStyle",
        description = "Запрос на изменение стиля ответов"
    ),

    COMMAND(
        wireName = "Command",
        requiresCommand = true,
        description = "Явная команда, начинающаяся с '/'"
    );

    companion object {
        fun allWireNames(): List<String> = entries.map { it.wireName }
    }
}
