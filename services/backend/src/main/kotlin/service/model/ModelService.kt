package com.katorabian.service.model

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


    suspend fun <T> withInference(
        model: ModelDescriptor,
        block: suspend () -> T
    ): T {
        val state = stateRef(model.id)
        state.set(ModelRuntimeState.BUSY)
        try {
            val result = block()
            state.set(ModelRuntimeState.READY)
            return result
        } catch (e: Exception) {
            state.set(ModelRuntimeState.ERROR)
            throw e
        }
    }
}
