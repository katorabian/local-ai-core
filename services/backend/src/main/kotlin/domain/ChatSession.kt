package com.katorabian.domain

import java.time.Instant
import java.util.UUID

data class ChatSession(
    val id: UUID,
    val model: String,
    val createdAt: Instant
)