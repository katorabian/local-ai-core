package com.katorabian.service.model

import com.katorabian.llm.LlmClient

data class ModelDescriptor(
    val id: String,
    val role: ModelRole,
    val client: LlmClient,
    val isLocal: Boolean
)