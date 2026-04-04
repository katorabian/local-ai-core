package com.katorabian.application

import com.katorabian.core.chat.ChatEngine
import com.katorabian.core.model.ModelSelector
import com.katorabian.core.repository.ChatRepository
import com.katorabian.infra.llm.providers.llamacpp.LlamaModel
import com.katorabian.infra.storage.InMemoryChatRepository
import com.katorabian.llm.gatekeeper.LlmGatekeeper
import com.katorabian.llm.llamacpp.LlamaChatServer
import com.katorabian.llm.llamacpp.LlamaCppClient
import com.katorabian.llm.llamacpp.LlamaGatekeeperServer
import com.katorabian.service.chat.ChatService
import com.katorabian.service.input.CommandExecutor
import com.katorabian.service.input.UserInputProcessor
import com.katorabian.service.message.ChatMessageService
import com.katorabian.service.orchestration.SessionExecutionManager
import com.katorabian.service.prompt.PromptAssembler
import com.katorabian.core.prompt.PromptBuilder
import com.katorabian.domain.Utils.toFile
import com.katorabian.prompt.PromptConfigFactory
import com.katorabian.service.session.ChatSessionService
import java.io.File

object AppModule {

    fun build(): AppComponents {

        val llamaDir = File("F:/llm/llama.cpp-12.4")

        // ===== CHAT (GPU) =====
        val chatServer = LlamaChatServer(
            llamaDir = llamaDir,
            modelPath = ModelPresets.LocalChat.modelPath.toFile(),
            port = 8081
        )
        val chatClient = LlamaCppClient(chatServer, "http://localhost:${chatServer.port}")
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
        val gatekeeperClient = LlamaCppClient(gatekeeperServer, "http://localhost:${gatekeeperServer.port}")
        val gatekeeperModel = LlamaModel(
            descriptor = ModelPresets.Gatekeeper,
            client = gatekeeperClient
        )
        val gatekeeper = LlmGatekeeper(
            model = gatekeeperModel
        )
        // ===== SELECTOR =====
        val modelSelector = ModelSelector(
            chatModel = chatModel,
            reasoningModel = null
        )


        // ===== STORAGE =====
        val repo: ChatRepository = InMemoryChatRepository()
        val sessionService = ChatSessionService(repo)
        val messageService = ChatMessageService(repo)

        // ===== PROMPT =====
        val promptAssembler = PromptAssembler(PromptConfigFactory())
        val promptBuilder = PromptBuilder(promptAssembler)

        // ===== INPUT =====
        val commandExecutor = CommandExecutor(sessionService)
        val inputProcessor = UserInputProcessor(
            commandExecutor = commandExecutor,
            sessionService = sessionService
        )

        // ===== ENGINE =====
        val engine = ChatEngine(
            modelSelector = modelSelector,
            gatekeeper = gatekeeper,
            messageService = messageService,
            promptBuilder = promptBuilder,
            inputProcessor = inputProcessor
        )

        val executionManager = SessionExecutionManager()

        val chatService = ChatService(
            engine = engine,
            sessionService = sessionService,
            executionManager = executionManager,
            messageService = messageService
        )

        return AppComponents(
            chatService = chatService,
            gatekeeper = gatekeeper,
            engine = engine
        )
    }
}