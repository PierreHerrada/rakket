package com.rakket.config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.*
import org.jetbrains.exposed.sql.Database
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("com.rakket.config.Database")

fun Application.configureDatabase(config: AppConfig) {
    logger.info("Connecting to database: ${config.databaseUrl}")

    val hikariConfig = HikariConfig().apply {
        jdbcUrl = config.databaseUrl
        username = config.databaseUser
        password = config.databasePassword
        driverClassName = "org.postgresql.Driver"
        maximumPoolSize = 10
        minimumIdle = 2
        idleTimeout = 60_000
        connectionTimeout = 30_000
        maxLifetime = 1_800_000
        isAutoCommit = false
    }

    val dataSource = HikariDataSource(hikariConfig)
    Database.connect(dataSource)

    logger.info("Database connected successfully")
}
