package com.katorabian.service.model

import com.katorabian.service.input.UserIntent

class ModelRouter(
    private val models: List<ModelDescriptor>,
    private val fallbackOrder: List<ModelRole>
) {

    fun resolveLocal(
        intent: UserIntent,
        modelService: ModelService
    ): ModelDescriptor {

        val desiredRole: ModelRole = roleForIntent(intent)

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

    @Deprecated("Заглушка пока я не придумал как привезти сюда Remote")
    fun resolveRemote(
        intent: UserIntent,
        modelService: ModelService
    ): ModelDescriptor {
        error("Remote models are not configured yet")
    }

    fun allModels(): List<ModelDescriptor> = models


    private fun roleForIntent(intent: UserIntent): ModelRole =
        when (intent) {

            UserIntent.Chat ->
                ModelRole.CHAT

            UserIntent.Intimate ->
                ModelRole.CHAT

            UserIntent.Code ->
                ModelRole.SMART_REASONING

            is UserIntent.Command, // just for info. wont execute
            is UserIntent.ChangeStyle ->
                ModelRole.GATEKEEPER

        }
}