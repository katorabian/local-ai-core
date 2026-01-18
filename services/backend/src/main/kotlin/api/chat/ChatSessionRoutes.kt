package com.katorabian.api.chat

import com.katorabian.service.chat.ChatService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.*

fun Route.chatSessionRoutes(chatService: ChatService) {

    post("/api/v1/chat/sessions") { _ ->
        val req = call.receive<CreateSessionRequest>()
        val session = chatService.createSession(
            model = req.model
        )
        call.respond(CreateSessionResponse(session.id.toString()))
    }

    post("/api/v1/chat/sessions/{id}/messages") { _ ->
        val sessionId = UUID.fromString(call.parameters["id"])
        val req = call.receive<SendMessageRequest>()

        val response = chatService.sendMessage(sessionId, req.message)
        call.respond(mapOf("content" to response.content))
    }

    get("/api/v1/chat/sessions") {
        val sessions = chatService.getAllSessions()
        call.respond(
            sessions.map {
                ChatSessionDto(
                    id = it.id.toString(),
                    model = it.model,
                    createdAt = it.createdAt.toString()
                )
            }
        )
    }

    get("/api/v1/chat/sessions/{id}") {
        val sessionId = UUID.fromString(call.parameters["id"])
        val session = chatService.getSession(sessionId)
            ?: return@get call.respondText(
                "Session not found",
                status = HttpStatusCode.NotFound
            )

        call.respond(
            ChatSessionDto(
                id = session.id.toString(),
                model = session.model,
                createdAt = session.createdAt.toString()
            )
        )
    }

    get("/api/v1/chat/sessions/{id}/messages") {
        val sessionId = UUID.fromString(call.parameters["id"])
        val messages = chatService.getSessionMessages(sessionId)

        call.respond(
            messages.map {
                ChatMessageDto(
                    id = it.id.toString(),
                    role = it.role.name.lowercase(),
                    content = it.content,
                    createdAt = it.createdAt.toString()
                )
            }
        )
    }

}