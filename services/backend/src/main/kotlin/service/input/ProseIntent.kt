package com.katorabian.service.input

import com.katorabian.prompt.BehaviorPrompt

sealed interface ProseIntent {

    data class ChangeStyle(
        val preset: BehaviorPrompt.Preset
    ) : ProseIntent
}
