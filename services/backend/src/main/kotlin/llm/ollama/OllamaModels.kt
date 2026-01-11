package com.katorabian.llm.ollama

import kotlinx.serialization.Serializable

@Serializable
data class OllamaRequest(
    val model: String,
    val prompt: String,
    val stream: Boolean
)

@Serializable
data class OllamaResponse(
    val response: String,
    val done: Boolean
)