package com.katorabian.application

import com.katorabian.api.chat.chatSessionRoutes
import com.katorabian.api.chat.chatStreamRoute
import com.katorabian.api.model.modelRoutes
import com.katorabian.llm.ollama.OllamaClient
import com.katorabian.service.prompt.PromptAssembler
import com.katorabian.prompt.PromptConfigFactory
import com.katorabian.service.message.ChatMessageService
import com.katorabian.service.chat.ChatService
import com.katorabian.service.session.ChatSessionService
import com.katorabian.service.input.CommandExecutor
import com.katorabian.service.input.CommandParser
import com.katorabian.service.prompt.PromptService
import com.katorabian.service.input.ProseIntentDetector
import com.katorabian.service.input.UserInputProcessor
import com.katorabian.service.model.ModelDescriptor
import com.katorabian.service.model.ModelRouter
import com.katorabian.service.model.ModelService
import com.katorabian.storage.ChatSessionStore
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
    val ollamaClient = OllamaClient()
    val store = ChatSessionStore()
    val modelRouter = ModelRouter(
        models = mapOf(
            // id модели = то, что сейчас хранится в ChatSession.model
            "llama3.2:3b" to ModelDescriptor(
                id = "llama3.2:3b",
                client = ollamaClient,
            )
        )
    )

    val modelService = ModelService()

    val sessionService = ChatSessionService(store)
    val messageService = ChatMessageService(store)

    val promptAssembler = PromptAssembler(PromptConfigFactory())
    val promptService = PromptService(store, promptAssembler)

    val commandParser = CommandParser()
    val commandExecutor = CommandExecutor(sessionService)
    val intentDetector = ProseIntentDetector()

    val inputProcessor = UserInputProcessor(
        commandParser = commandParser,
        commandExecutor = commandExecutor,
        intentDetector = intentDetector,
        sessionService = sessionService
    )

    val chatService = ChatService(
        sessionService = sessionService,
        messageService = messageService,
        promptService = promptService,
        modelRouter = modelRouter,
        modelService = modelService,
        inputProcessor = inputProcessor
    )

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

            modelRoutes(modelService)
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
