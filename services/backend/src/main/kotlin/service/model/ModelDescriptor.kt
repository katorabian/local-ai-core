package com.katorabian.service.model

import com.katorabian.llm.LlmClient

data class ModelDescriptor(
    val id: String,
    val client: LlmClient
)
