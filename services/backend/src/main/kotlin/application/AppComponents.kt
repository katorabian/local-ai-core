package com.katorabian.application

import com.katorabian.core.chat.ChatEngine
import com.katorabian.llm.gatekeeper.LlmGatekeeper
import com.katorabian.service.chat.ChatService

data class AppComponents(
    val chatService: ChatService,
    val gatekeeper: LlmGatekeeper,
    val engine: ChatEngine
)