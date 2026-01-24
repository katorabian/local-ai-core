package com.katorabian.application

import com.katorabian.llm.llamacpp.LlamaCppClient
import com.katorabian.service.model.ModelDescriptor
import com.katorabian.service.model.ModelRole

object ModelPresets {

    fun smartChat(llamaClient: LlamaCppClient) =
        ModelDescriptor(
            id = "qwen2.5-14b",
            role = ModelRole.SMART_CHAT,
            client = llamaClient,
            isLocal = true
        )
}