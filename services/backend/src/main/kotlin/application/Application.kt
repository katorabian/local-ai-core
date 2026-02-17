package com.katorabian.application

import com.katorabian.api.chat.chatSessionRoutes
import com.katorabian.api.chat.chatStreamRoute
import com.katorabian.api.model.modelRoutes
import com.katorabian.domain.Utils.toFile
import com.katorabian.llm.gatekeeper.LlmGatekeeper
import com.katorabian.llm.llamacpp.LlamaChatServer
import com.katorabian.llm.llamacpp.LlamaCppClient
import com.katorabian.llm.llamacpp.LlamaGatekeeperServer
import com.katorabian.prompt.PromptConfigFactory
import com.katorabian.service.chat.ChatService
import com.katorabian.service.input.CommandExecutor
import com.katorabian.service.input.UserInputProcessor
import com.katorabian.service.message.ChatMessageService
import com.katorabian.service.model.ModelRole
import com.katorabian.service.model.ModelRouter
import com.katorabian.service.model.ModelService
import com.katorabian.service.prompt.PromptAssembler
import com.katorabian.service.prompt.PromptService
import com.katorabian.service.session.ChatSessionService
import com.katorabian.storage.ChatSessionStore
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.runBlocking
import java.io.File

fun main() {
    val llamaDir = File("F:/llm/llama.cpp-12.4")

    // ===== CHAT (GPU) =====
    val chatServer = LlamaChatServer(
        llamaDir = llamaDir,
        modelPath = ModelPresets.LocalChat.modelPath.toFile(),
        port = 8081
    )
    val chatClient = LlamaCppClient(chatServer, "http://localhost:8081")

    // ===== GATEKEEPER (CPU) =====
    val gatekeeperServer = LlamaGatekeeperServer(
        llamaDir = llamaDir,
        modelPath = ModelPresets.Gatekeeper.modelPath.toFile(),
        port = 8082
    )
    val gatekeeperClient = LlamaCppClient(gatekeeperServer, "http://localhost:8082")
    val gatekeeper = LlmGatekeeper(
        descriptor = ModelPresets.Gatekeeper,
        llmClient = gatekeeperClient
    ).also {
        runBlocking { it.warmUp() }
    }


    // ===== OTHER =====
    val models = listOf(ModelPresets.LocalChat)
    val modelService = ModelService(models)
    val modelRouter = ModelRouter(
        models = models,
        fallbackOrder = listOf(ModelRole.CHAT)
    )

    val store = ChatSessionStore()
    val sessionService = ChatSessionService(store)
    val messageService = ChatMessageService(store)

    val promptAssembler = PromptAssembler(PromptConfigFactory())
    val promptService = PromptService(store, promptAssembler)

    // commands
    val commandExecutor = CommandExecutor(sessionService)
    val inputProcessor = UserInputProcessor(
        commandExecutor = commandExecutor,
        sessionService = sessionService
    )

    val chatService = ChatService(
        llmClient = chatClient,
        sessionService = sessionService,
        messageService = messageService,
        promptService = promptService,
        modelRouter = modelRouter,
        modelService = modelService,
        inputProcessor = inputProcessor,
        gatekeeper = gatekeeper
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

            chatSessionRoutes(chatService)
            chatStreamRoute(chatService)

            modelRoutes(modelService)
        }
    }.start(wait = true)

}