package com.katorabian.core.model

import com.katorabian.domain.ChatMessage

interface Model {

    val id: String

    suspend fun generate(
        messages: List<ChatMessage>
    ): String

    suspend fun stream(
        messages: List<ChatMessage>,
        onToken: suspend (String) -> Unit
    )
}