package com.katorabian.application

import com.katorabian.api.chat.chatSessionRoutes
import com.katorabian.api.chat.chatStreamRoute
import com.katorabian.llm.ollama.OllamaClient
import com.katorabian.service.ChatService
import com.katorabian.service.ChatSessionStore
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

fun main() {
    val llmClient = OllamaClient()
    val store = ChatSessionStore()
    val chatService = ChatService(llmClient, store)

    embeddedServer(Netty, port = 8080) {
        install(ContentNegotiation) {
            json()
        }

        install(CORS) {
            allowHost("localhost:5173")
            allowMethod(HttpMethod.Get)
            allowMethod(HttpMethod.Post)
            allowHeader(HttpHeaders.ContentType)
            allowCredentials = true
        }

        routing {
            get("/health") {
                call.respondText("OK")
            }

            post("/api/v1/chat") { _ ->
                val req = call.receive<ChatRequest>()

                val session = chatService.createSession(req.model)
                val response = chatService.sendMessage(session.id, req.message)

                call.respond(
                    ChatResponse(
                        content = response.content
                    )
                )
            }

            chatSessionRoutes(chatService)
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
