package com.katorabian.api.chat

import com.katorabian.domain.chat.SendMessageRequest
import com.katorabian.service.chat.ChatService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.receive
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.*

fun Route.chatStreamRoute(chatService: ChatService) {

    post("/api/v1/chat/sessions/{id}/stream") { _ ->

        val sessionId = UUID.fromString(call.parameters["id"])
        val message = call.receive<SendMessageRequest>().message

        call.respondTextWriter(
            contentType = ContentType.Text.EventStream
        ) {
            chatService.streamMessage(
                sessionId = sessionId,
                userQuery = message
            ) { event ->
                val json = ChatEventEncoder.encode(event)
                write("event: message\n")
                write("data: $json\n\n")
                flush()
            }

            write("event: done\ndata: {}\n\n")
            flush()
        }
    }
}
