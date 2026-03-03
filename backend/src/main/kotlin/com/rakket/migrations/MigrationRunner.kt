package com.rakket.migrations

import com.rakket.config.AppConfig
import org.slf4j.LoggerFactory
import java.io.File
import java.sql.DriverManager

object MigrationRunner {
    private val logger = LoggerFactory.getLogger(MigrationRunner::class.java)

    fun run(config: AppConfig) {
        logger.info("Running database migrations...")

        val connection = DriverManager.getConnection(
            config.databaseUrl,
            config.databaseUser,
            config.databasePassword
        )

        connection.use { conn ->
            conn.autoCommit = false

            // Create schema_version table if it doesn't exist
            conn.createStatement().execute(
                """
                CREATE TABLE IF NOT EXISTS schema_version (
                    version INTEGER PRIMARY KEY,
                    description TEXT NOT NULL,
                    applied_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
                )
                """.trimIndent()
            )
            conn.commit()

            // Get current version
            val currentVersion = conn.createStatement()
                .executeQuery("SELECT COALESCE(MAX(version), 0) FROM schema_version")
                .let { rs ->
                    rs.next()
                    rs.getInt(1)
                }

            logger.info("Current schema version: $currentVersion")

            // Find migration files
            val migrationsDir = findMigrationsDir()
            if (migrationsDir == null) {
                logger.info("No migrations directory found, skipping.")
                return
            }

            val migrationFiles = migrationsDir.listFiles()
                ?.filter { it.name.endsWith(".sql") }
                ?.sortedBy { it.name }
                ?: emptyList()

            for (file in migrationFiles) {
                val version = extractVersion(file.name)
                if (version != null && version > currentVersion) {
                    val description = extractDescription(file.name)
                    logger.info("Applying migration V$version: $description")

                    val sql = file.readText()
                    conn.createStatement().execute(sql)

                    conn.prepareStatement(
                        "INSERT INTO schema_version (version, description) VALUES (?, ?)"
                    ).apply {
                        setInt(1, version)
                        setString(2, description)
                        executeUpdate()
                    }

                    conn.commit()
                    logger.info("Migration V$version applied successfully")
                }
            }

            logger.info("All migrations applied.")
        }
    }

    private fun findMigrationsDir(): File? {
        val paths = listOf(
            "db/migrations",
            "../db/migrations",
            "/app/db/migrations"
        )
        return paths.map { File(it) }.firstOrNull { it.exists() && it.isDirectory }
    }

    private fun extractVersion(filename: String): Int? {
        val match = Regex("^V(\\d+)__").find(filename)
        return match?.groupValues?.get(1)?.toIntOrNull()
    }

    private fun extractDescription(filename: String): String {
        return filename
            .replace(Regex("^V\\d+__"), "")
            .replace(".sql", "")
            .replace("_", " ")
    }
}
