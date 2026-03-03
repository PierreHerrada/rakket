package com.rakket

import com.rakket.config.AppConfig
import com.rakket.config.configureDatabase
import com.rakket.config.configureSerialization
import com.rakket.migrations.MigrationRunner
import com.rakket.routes.configureRouting
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.defaultheaders.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.http.*
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("com.rakket.Application")

fun main() {
    val config = AppConfig.load()

    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        module(config)
    }.start(wait = true)
}

fun Application.module(config: AppConfig) {
    logger.info("Starting Rakket v0.1.0")

    install(DefaultHeaders)
    install(CallLogging)
    install(CORS) {
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
        anyHost()
    }
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            logger.error("Unhandled exception", cause)
            call.respond(
                io.ktor.http.HttpStatusCode.InternalServerError,
                mapOf("error" to (cause.message ?: "Internal server error"))
            )
        }
    }

    configureSerialization()
    configureDatabase(config)
    MigrationRunner.run(config)
    configureRouting()

    logger.info("Rakket started successfully")
}
