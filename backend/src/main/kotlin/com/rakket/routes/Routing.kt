package com.rakket.routes

import com.rakket.config.AppConfig
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*

fun Application.configureRouting(config: AppConfig) {
    install(Sessions) {
        cookie<UserSession>("RAKKET_SESSION") {
            cookie.path = "/"
            cookie.maxAgeInSeconds = 60 * 60 * 24 * 7 // 7 days
            cookie.httpOnly = true
        }
    }

    routing {
        get("/api/health") {
            call.respond(HttpStatusCode.OK, mapOf("status" to "ok", "version" to "0.1.0"))
        }

        authRoutes(config)
        playerRoutes()
        tournamentRoutes()
    }
}
