package com.katorabian.api.chat

import com.katorabian.domain.ChatMessage
import com.katorabian.domain.enum.Role
import com.katorabian.service.ChatService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Instant
import java.util.*

fun Route.chatStreamRoute(chatService: ChatService) {

    get("/api/v1/chat/stream") {

        val model = call.request.queryParameters["model"]
            ?: return@get call.respond(HttpStatusCode.BadRequest, "model required")

        val message = call.request.queryParameters["message"]
            ?: return@get call.respond(HttpStatusCode.BadRequest, "message required")

        call.response.cacheControl(CacheControl.NoCache(null))
        call.respondTextWriter(
            contentType = ContentType.Text.EventStream
        ) {
            val session = chatService.createSession(model)

            val userMessage = ChatMessage(
                id = UUID.randomUUID(),
                sessionId = session.id,
                role = Role.USER,
                content = message,
                createdAt = Instant.now()
            )

            chatService.streamMessage(
                session = session,
                userMessage = userMessage
            ) { token ->
                val payload = Json.encodeToString(
                    mapOf("text" to token)
                )

                write("event: token\n")
                write("time: ${Instant.now()}\n")
                write("data: $payload\n\n")
                flush()
            }

            write("event: done\n")
            write("data: {}\n\n")
            flush()
        }
    }
}
