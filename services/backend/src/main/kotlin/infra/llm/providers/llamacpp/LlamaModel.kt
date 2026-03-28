package com.katorabian.infra.llm.providers.llamacpp

import com.katorabian.core.model.Model
import com.katorabian.domain.ChatMessage
import com.katorabian.llm.LlmClient
import com.katorabian.service.model.ModelDescriptor

class LlamaModel(
    private val descriptor: ModelDescriptor,
    private val client: LlmClient
) : Model {

    override val id: String = descriptor.id

    override suspend fun generate(
        messages: List<ChatMessage>
    ): String {
        return client.generate(
            model = descriptor.id,
            messages = messages
        )
    }

    override suspend fun stream(
        messages: List<ChatMessage>,
        onToken: suspend (String) -> Unit
    ) {
        client.stream(
            model = descriptor.id,
            messages = messages,
            onToken = onToken
        )
    }
}