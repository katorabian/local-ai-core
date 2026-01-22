package com.katorabian.service.model

class ModelRouter(
    private val models: Map<String, ModelDescriptor>
) {

    fun resolve(modelId: String): ModelDescriptor =
        models[modelId]
            ?: error("Model not found: $modelId")
}