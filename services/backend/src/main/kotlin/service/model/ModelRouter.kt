package com.katorabian.service.model

class ModelRouter(
    private val models: List<ModelDescriptor>,
    private val fallbackOrder: List<ModelRole>
) {

    fun resolve(
        input: String,
        modelService: ModelService
    ): ModelDescriptor {

        val desiredRole = InputClassifier.classify(input)

        // 1. Пробуем модель нужной роли
        models.firstOrNull {
            val isDesiredRole = it.role == desiredRole
            val isNotErrorState = modelService.getState(it.id) != ModelRuntimeState.ERROR
            isDesiredRole && isNotErrorState
        }?.let { return it }

        // 2. Fallback по ролям
        for (role in fallbackOrder) {
            models.firstOrNull { it ->
                val isRoleMatch = it.role == role
                val isNotErrorState = modelService.getState(it.id) != ModelRuntimeState.ERROR
                isRoleMatch && isNotErrorState
            }?.let { return it }
        }

        error("No available models")
    }

    fun allModels(): List<ModelDescriptor> = models
}
