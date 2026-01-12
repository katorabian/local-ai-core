package com.katorabian.application

import com.katorabian.api.chat.chatStreamRoute
import com.katorabian.llm.ollama.OllamaClient
import com.katorabian.service.ChatService
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

fun main() {
    val llmClient = OllamaClient()
    val chatService = ChatService(llmClient)

    embeddedServer(Netty, port = 8080) {
        install(ContentNegotiation) {
            json()
        }

        routing {
            get("/health") {
                call.respondText("OK")
            }

            post("/api/v1/chat") {
                val req = call.receive<ChatRequest>()

                val session = chatService.createSession(req.model)
                val response = chatService.sendMessage(session, req.message)

                call.respond(
                    ChatResponse(
                        content = response.content
                    )
                )
            }

            chatStreamRoute(chatService)
        }
    }.start(wait = true)
}

@Serializable
data class ChatRequest(
    val model: String,
    val message: String
)

@Serializable
data class ChatResponse(
    val content: String
)
