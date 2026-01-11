package com.katorabian

import com.katorabian.ollama.OllamaClient
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.plugins.contentnegotiation.*
import kotlinx.serialization.json.Json
import io.ktor.serialization.kotlinx.json.*

fun main() {
    embeddedServer(Netty, port = 8080) {
        install(ContentNegotiation) {
            json(Json { prettyPrint = true })
        }

        routing {
            get("/health") {
                call.respondText("OK")
            }

            get("/chat") {
                val prompt = call.request.queryParameters["prompt"]
                    ?: return@get call.respondText("Missing prompt")

                val response = OllamaClient.chat(prompt)
                call.respond(response)
            }
        }
    }.start(wait = true)
}
