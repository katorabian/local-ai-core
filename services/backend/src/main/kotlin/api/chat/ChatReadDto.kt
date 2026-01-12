package com.katorabian.api.chat

import kotlinx.serialization.Serializable

@Serializable
data class ChatSessionDto(
    val id: String,
    val model: String,
    val createdAt: String
)

@Serializable
data class ChatMessageDto(
    val id: String,
    val role: String,
    val content: String,
    val createdAt: String
)
