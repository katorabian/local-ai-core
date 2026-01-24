package com.katorabian.service.model

import com.katorabian.domain.ChatMessage
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference

class ModelService {

    private val states =
        ConcurrentHashMap<String, AtomicReference<ModelRuntimeState>>()

    fun getState(modelId: String): ModelRuntimeState =
        states[modelId]?.get() ?: ModelRuntimeState.COLD

    fun mark(modelId: String, state: ModelRuntimeState) {
        states.getOrPut(modelId) { AtomicReference(ModelRuntimeState.COLD) }
            .set(state)
    }

    suspend fun warmUp(model: ModelDescriptor) {
        val state = states.getOrPut(model.id) {
            AtomicReference(ModelRuntimeState.COLD)
        }

        val wasCold = state.compareAndSet(
            ModelRuntimeState.COLD,
            ModelRuntimeState.WARMING_UP
        )
        if (!wasCold) return

        try {
            // минимальный запрос для прогрева
            model.client.generate(
                model = model.id,
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
        model: ModelDescriptor,
        block: suspend () -> T
    ): T {
        val state = states.getOrPut(model.id) {
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
