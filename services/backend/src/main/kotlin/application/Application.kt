package com.katorabian.application

import com.katorabian.api.chat.chatSessionRoutes
import com.katorabian.api.chat.chatStreamRoute
import com.katorabian.domain.Constants.MAX_NETTY_REQUEST_TIMEOUT
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

fun main() {
    val components = AppModule.build()

    val chatService = components.chatService
    val gatekeeper = components.gatekeeper


    embeddedServer(Netty,
        port = 8080,
        configure = { // Таймаут на запись ответа, чтобы соединение не обрывалось
            responseWriteTimeoutSeconds = MAX_NETTY_REQUEST_TIMEOUT
        }
    ) {
        install(ContentNegotiation) {
            json()
        }

        install(CORS) {
            allowHost("localhost:5173") // Разрешаем фронту ходить к бэку
            // Разрешённые HTTP-методы
            allowMethod(HttpMethod.Get)
            allowMethod(HttpMethod.Post)
            // Разрешённые заголовки
            allowHeader(HttpHeaders.ContentType)
            // Разрешаем куки / авторизацию
            allowCredentials = true
        }

        routing {
            // Простой health-check (для мониторинга или docker healthcheck)
            get("/health") {
                call.respondText("OK")
            }

            chatSessionRoutes(chatService) // REST API для работы с чат-сессиями
            chatStreamRoute(chatService) // SSE стриминг ответов модели
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