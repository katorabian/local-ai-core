package com.katorabian.api.chat

import kotlinx.serialization.Serializable


@Serializable
data class CreateSessionRequest(val model: String)

@Serializable
data class CreateSessionResponse(val sessionId: String)

@Serializable
data class SendMessageRequest(val message: String)
