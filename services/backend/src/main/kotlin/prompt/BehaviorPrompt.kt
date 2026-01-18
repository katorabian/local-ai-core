package com.katorabian.prompt

object BehaviorPrompt {

    fun forPreset(preset: Preset): List<String> =
        when (preset) {
            Preset.NEUTRAL -> emptyList()

            Preset.SARCASTIC -> listOf(
                "Отвечай саркастично и язвительно.",
                "Необходимо отвечать с явной иронией и нахальством."
            )

            Preset.FORMAL -> listOf(
                "Используй формальный и деловой стиль общения.",
                "Избегай разговорных выражений."
            )

            Preset.CONCISE -> listOf(
                "Отвечай максимально кратко.",
                "Избегай лишних пояснений, если они не запрошены."
            )
        }


    enum class Preset {
        NEUTRAL,
        SARCASTIC,
        FORMAL,
        CONCISE
    }
}