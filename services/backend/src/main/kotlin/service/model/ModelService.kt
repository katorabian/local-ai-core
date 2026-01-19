package com.katorabian.service.model

import com.katorabian.domain.ChatMessage
import com.katorabian.llm.LlmClient
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference

class ModelService(
    private val llmClient: LlmClient
) {

    private val states = ConcurrentHashMap<String, AtomicReference<ModelRuntimeState>>()

    fun getState(model: String): ModelRuntimeState =
        states[model]?.get() ?: ModelRuntimeState.COLD

    suspend fun warmUp(model: String) {
        val state = states.getOrPut(model) {
            AtomicReference(ModelRuntimeState.COLD)
        }

        val wasCold = state.compareAndSet(
            ModelRuntimeState.COLD,
            ModelRuntimeState.WARMING_UP
        )
        if (!wasCold) return

        try {
            llmClient.generate(
                model = model,
                messages = listOf(
                    ChatMessage.system("Warm-up")
                )
            )
            state.set(ModelRuntimeState.READY)
        } catch (e: Exception) {
            state.set(ModelRuntimeState.ERROR)
            throw e
        }
    }

    suspend fun <T> withInference(
        model: String,
        block: suspend () -> T
    ): T {
        val state = states.getOrPut(model) {
            AtomicReference(ModelRuntimeState.COLD)
        }

        if (state.get() == ModelRuntimeState.COLD) {
            warmUp(model)
        }

        state.set(ModelRuntimeState.BUSY)
        try {
            return block()
        } finally {
            state.set(ModelRuntimeState.READY)
        }
    }
}
