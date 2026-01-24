package com.katorabian.service.model

object InputClassifier {

    fun classify(input: String): ModelRole =
        when {
            looksLikeCommand(input) -> ModelRole.SYSTEM
            looksLikeCode(input) -> ModelRole.SMART_REASONING
            input.length < 120 -> ModelRole.FAST_CHAT
            else -> ModelRole.SMART_REASONING
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
