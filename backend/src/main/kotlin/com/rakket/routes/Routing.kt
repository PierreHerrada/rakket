package com.rakket.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    routing {
        get("/api/health") {
            call.respond(HttpStatusCode.OK, mapOf("status" to "ok", "version" to "0.1.0"))
        }
    }
}
