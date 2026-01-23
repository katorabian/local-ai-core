package com.katorabian.service.model

class ModelRouter(
    private val models: Map<String, ModelDescriptor>,
    private val fallbackOrder: List<String>
) {

    fun resolve(primaryModelId: String, modelService: ModelService): ModelDescriptor {
        // 1. Пробуем primary
        models[primaryModelId]?.let { primary ->
            val state = modelService.getState(primary.id)
            if (state != ModelRuntimeState.ERROR) {
                return primary
            }
        }

        // 2. Пробуем fallback по порядку
        for (fallbackId in fallbackOrder) {
            val candidate = models[fallbackId] ?: continue
            val state = modelService.getState(candidate.id)
            if (state != ModelRuntimeState.ERROR) {
                return candidate
            }
        }

        error("No available models")
    }

    fun allModels(): Collection<ModelDescriptor> = models.values
}
