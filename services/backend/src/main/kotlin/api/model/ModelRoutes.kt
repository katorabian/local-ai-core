package com.katorabian.api.model

import com.katorabian.service.model.ModelService
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.modelRoutes(
    modelService: ModelService
) {
    get("/api/v1/models/{model}/state") {
        val model = call.parameters["model"]!!
        call.respond(
            mapOf(
                "model" to model,
                "state" to modelService.getState(model).name
            )
        )
    }
}
