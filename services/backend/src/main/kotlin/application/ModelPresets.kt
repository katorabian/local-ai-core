package com.katorabian.application

import com.katorabian.service.model.ModelDescriptor

object ModelPresets {

    val LocalChat = ModelDescriptor(
        id = "local-chat",
        modelPath = "F:/llm/models/Mistral-Nemo-2407-12B-Thinking-Claude-Gemini-GPT5.2-Uncensored-HERETIC.Q8_0.gguf"
    )

    val Gatekeeper = ModelDescriptor(
        id = "gatekeeper",
        modelPath = "F:/llm/models/Qwen3-Gatekeeper-4B-f16-Q4_K_M.gguf"
    )
}