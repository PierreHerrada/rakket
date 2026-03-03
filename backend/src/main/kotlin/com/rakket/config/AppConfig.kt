package com.rakket.config

data class AppConfig(
    val databaseUrl: String,
    val databaseUser: String,
    val databasePassword: String,
    val slackBotToken: String,
    val slackSigningSecret: String,
    val slackClientId: String,
    val slackClientSecret: String,
    val slackChannelId: String,
    val appUrl: String,
    val timezone: String,
    val tournamentDay: String,
    val registrationTime: String,
    val tournamentTime: String,
    val matchFormat: String,
) {
    companion object {
        fun load(): AppConfig {
            return AppConfig(
                databaseUrl = env("DATABASE_URL", "jdbc:postgresql://localhost:5432/rakket"),
                databaseUser = env("DATABASE_USER", "rakket"),
                databasePassword = env("DATABASE_PASSWORD", "changeme"),
                slackBotToken = env("SLACK_BOT_TOKEN", ""),
                slackSigningSecret = env("SLACK_SIGNING_SECRET", ""),
                slackClientId = env("SLACK_CLIENT_ID", ""),
                slackClientSecret = env("SLACK_CLIENT_SECRET", ""),
                slackChannelId = env("SLACK_CHANNEL_ID", ""),
                appUrl = env("APP_URL", "http://localhost:8080"),
                timezone = env("TIMEZONE", "Europe/Paris"),
                tournamentDay = env("TOURNAMENT_DAY", "MONDAY"),
                registrationTime = env("REGISTRATION_TIME", "09:00"),
                tournamentTime = env("TOURNAMENT_TIME", "16:00"),
                matchFormat = env("MATCH_FORMAT", "BEST_OF_3"),
            )
        }

        private fun env(name: String, default: String): String {
            return System.getenv(name) ?: default
        }
    }
}
