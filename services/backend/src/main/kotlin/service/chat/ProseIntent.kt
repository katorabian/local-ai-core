package com.katorabian.service.chat

import com.katorabian.prompt.BehaviorPrompt

sealed interface ProseIntent {

    data class ChangeStyle(
        val preset: BehaviorPrompt.Preset
    ) : ProseIntent
}
