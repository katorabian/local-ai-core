package com.katorabian.core.model

import com.katorabian.service.gatekeeper.GatekeeperDecision
import com.katorabian.service.input.UserIntent

class ModelSelector(
    private val chatModel: Model,
    private val reasoningModel: Model? = null
) {

    fun select(decision: GatekeeperDecision): Model {
        return when (decision.intent) {
            UserIntent.Code ->
                reasoningModel ?: chatModel

            else ->
                chatModel
        }
    }
}