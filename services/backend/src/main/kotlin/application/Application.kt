package com.katorabian.application

import com.katorabian.api.chat.chatSessionRoutes
import com.katorabian.api.chat.chatStreamRoute
import com.katorabian.api.model.modelRoutes
import com.katorabian.llm.llamacpp.LlamaCppClient
import com.katorabian.llm.llamacpp.LlamaServerProcess
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
import com.katorabian.service.model.ModelRole
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
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

fun main() {
    val llamaServer = LlamaServerProcess(
        llamaDir = File("F:/llm/llama.cpp-12.4"),
        modelPath = File("F:/llm/models/Qwen2.5-14B-Q4_K_M.gguf")
    )

    val llamaClient = LlamaCppClient(llamaServer)
    val models = listOf(
        ModelPresets.smartChat(llamaClient)
    )

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

            chatSessionRoutes(chatService)
            chatStreamRoute(chatService)

            modelRoutes(modelService)
        }
    }.start(wait = true)

}