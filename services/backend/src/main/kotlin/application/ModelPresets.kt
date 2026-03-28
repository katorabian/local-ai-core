package com.katorabian.application

import com.katorabian.llm.gatekeeper.GatekeeperDescriptor
import com.katorabian.service.model.ModelDescriptor
import com.katorabian.service.model.ModelRole

object ModelPresets {

    val LocalChat = ModelDescriptor(
        id = "local-chat",
        role = ModelRole.CHAT,
        modelPath = "F:/llm/models/Mistral-Nemo-2407-12B-Thinking-Claude-Gemini-GPT5.2-Uncensored-HERETIC.Q8_0.gguf"
    )

    val Gatekeeper = ModelDescriptor(
        id = "gatekeeper",
        role = ModelRole.GATEKEEPER,
        modelPath = "F:/llm/models/Qwen3-Gatekeeper-4B-f16-Q4_K_M.gguf"
    )
}