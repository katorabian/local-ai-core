package com.katorabian.service.model

object InputClassifier {

    fun classify(input: String): ModelRole =
        when {
            looksLikeCommand(input) -> ModelRole.CHAT //ModelRole.SYSTEM
            looksLikeCode(input) -> ModelRole.CHAT //ModelRole.SMART_REASONING
            input.length < 120 -> ModelRole.CHAT
            else -> ModelRole.CHAT
        }

    private fun looksLikeCommand(input: String): Boolean =
        input.startsWith("/") ||
                input.contains("будь") ||
                input.contains("отвечай")

    private fun looksLikeCode(input: String): Boolean =
        input.contains("```") ||
                input.contains(";") ||
                input.contains("{") && input.contains("}")
}
