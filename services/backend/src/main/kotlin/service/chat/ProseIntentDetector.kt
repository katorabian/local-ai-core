package com.katorabian.service.chat

import com.katorabian.prompt.BehaviorPrompt

class ProseIntentDetector {

    fun detect(input: String): ProseIntent? {
        val text = input.lowercase()

        // TODO Временная реализация
        return when {
            text.contains("короче") ||
                    text.contains("кратко") ->
                ProseIntent.ChangeStyle(BehaviorPrompt.Preset.CONCISE)

            text.contains("формаль") ->
                ProseIntent.ChangeStyle(BehaviorPrompt.Preset.FORMAL)

            text.contains("саркаст") ->
                ProseIntent.ChangeStyle(BehaviorPrompt.Preset.SARCASTIC)

            text.contains("обычно") ||
                    text.contains("нормально") ->
                ProseIntent.ChangeStyle(BehaviorPrompt.Preset.NEUTRAL)

            else -> null
        }
    }
}
