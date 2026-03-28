package com.katorabian.application

import com.katorabian.api.chat.chatSessionRoutes
import com.katorabian.api.chat.chatStreamRoute
import com.katorabian.domain.Constants.MAX_NETTY_REQUEST_TIMEOUT
import com.katorabian.domain.Utils.toFile
import com.katorabian.infra.llm.providers.llamacpp.LlamaModel
import com.katorabian.llm.gatekeeper.LlmGatekeeper
import com.katorabian.llm.llamacpp.LlamaChatServer
import com.katorabian.llm.llamacpp.LlamaCppClient
import com.katorabian.llm.llamacpp.LlamaGatekeeperServer
import com.katorabian.prompt.PromptConfigFactory
import com.katorabian.service.chat.ChatService
import com.katorabian.service.input.CommandExecutor
import com.katorabian.service.input.UserInputProcessor
import com.katorabian.service.message.ChatMessageService
import com.katorabian.service.orchestration.SessionExecutionManager
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
import kotlinx.coroutines.launch
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
    val chatModel = LlamaModel(
        descriptor = ModelPresets.LocalChat,
        client = chatClient
    )

    // ===== GATEKEEPER (CPU) =====
    val gatekeeperServer = LlamaGatekeeperServer(
        llamaDir = llamaDir,
        modelPath = ModelPresets.Gatekeeper.modelPath.toFile(),
        port = 8082
    )
    val gatekeeperClient = LlamaCppClient(gatekeeperServer, "http://localhost:8082")
    val gatekeeperModel = LlamaModel(
        descriptor = ModelPresets.Gatekeeper,
        client = gatekeeperClient
    )

    val gatekeeper = LlmGatekeeper(
        model = gatekeeperModel
    )


    // ===== OTHER =====
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

    val executionManager = SessionExecutionManager()

    val chatService = ChatService(
        chatModel = chatModel,
        gatekeeper = gatekeeper,
        sessionService = sessionService,
        messageService = messageService,
        promptService = promptService,
        inputProcessor = inputProcessor,
        executionManager = executionManager
    )


    embeddedServer(Netty,
        port = 8080,
        configure = { responseWriteTimeoutSeconds = MAX_NETTY_REQUEST_TIMEOUT }
    ) {
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
        }

        environment.monitor.subscribe(ApplicationStarted) {
            // Асинхронный warm-up без блокировки main thread
            launch {
                try {
                    gatekeeper.warmUp()
                    println("Gatekeeper warm-up completed")
                } catch (e: Exception) {
                    println("Gatekeeper warm-up failed: ${e.message}")
                }
            }
        }
    }.start(wait = true)

}