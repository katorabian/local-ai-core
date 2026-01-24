package com.katorabian.service.model

import com.katorabian.domain.ChatMessage
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference

class ModelService(
    models: List<ModelDescriptor> = emptyList()
) {
    private val states = ConcurrentHashMap<String, AtomicReference<ModelRuntimeState>>()
    init {
        models.forEach { model ->
            states.putIfAbsent(
                model.id,
                AtomicReference(ModelRuntimeState.COLD)
            )
        }
    }

    fun getState(modelId: String): ModelRuntimeState =
        states[modelId]?.get() ?: ModelRuntimeState.COLD

    fun mark(modelId: String, state: ModelRuntimeState) {
        val ref = states[modelId] ?: error("Model state not initialized: $modelId")
        ref.set(state)
    }

    private fun stateRef(modelId: String): AtomicReference<ModelRuntimeState> =
        states.getOrPut(modelId) {
            AtomicReference(ModelRuntimeState.COLD)
        }

    suspend fun warmUp(model: ModelDescriptor) {
        val state = stateRef(model.id)
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
        val state = stateRef(model.id)
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
