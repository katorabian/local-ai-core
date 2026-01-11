package com.katorabian.domain

import com.katorabian.domain.enum.Role
import java.time.Instant
import java.util.*

data class ChatMessage(
    val id: UUID,
    val sessionId: UUID,
    val role: Role,
    val content: String,
    val createdAt: Instant
)
