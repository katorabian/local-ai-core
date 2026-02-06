package com.katorabian.service.input

import com.katorabian.prompt.BehaviorPrompt
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface UserIntent {
    @Serializable @SerialName("chat")
    object Chat : UserIntent

    @Serializable @SerialName("code")
    object Code : UserIntent

    @Serializable @SerialName("change_style")
    data class ChangeStyle(val preset: BehaviorPrompt.Preset) : UserIntent

    @Serializable @SerialName("command")
    data class Command(val name: String) : UserIntent
}