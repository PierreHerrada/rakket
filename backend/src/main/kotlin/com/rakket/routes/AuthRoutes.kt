package com.rakket.routes

import com.rakket.config.AppConfig
import com.rakket.db.Players
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("com.rakket.routes.AuthRoutes")

@Serializable
data class UserSession(val slackUserId: String, val displayName: String, val avatarUrl: String? = null)

@Serializable
data class SlackOAuthResponse(
    val ok: Boolean,
    @SerialName("access_token") val accessToken: String? = null,
    @SerialName("authed_user") val authedUser: SlackAuthedUser? = null,
    val team: SlackTeam? = null,
    val error: String? = null,
)

@Serializable
data class SlackAuthedUser(
    val id: String,
    @SerialName("access_token") val accessToken: String? = null,
)

@Serializable
data class SlackTeam(val id: String, val name: String? = null)

@Serializable
data class SlackUserInfo(
    val ok: Boolean,
    val user: SlackUser? = null,
    val error: String? = null,
)

@Serializable
data class SlackUser(
    val id: String,
    val name: String? = null,
    val profile: SlackProfile? = null,
    @SerialName("real_name") val realName: String? = null,
)

@Serializable
data class SlackProfile(
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("real_name") val realName: String? = null,
    @SerialName("image_192") val image192: String? = null,
)

private val httpClient = HttpClient(CIO) {
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true })
    }
}

fun Route.authRoutes(config: AppConfig) {
    route("/api/auth") {
        get("/slack") {
            val redirectUri = "${config.appUrl}/api/auth/callback"
            val scopes = "openid,profile"
            val url = "https://slack.com/oauth/v2/authorize" +
                "?client_id=${config.slackClientId}" +
                "&user_scope=$scopes" +
                "&redirect_uri=${redirectUri.encodeURLParameter()}"
            call.respondRedirect(url)
        }

        get("/callback") {
            val code = call.parameters["code"]
            if (code == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing authorization code"))
                return@get
            }

            val redirectUri = "${config.appUrl}/api/auth/callback"

            val tokenResponse: SlackOAuthResponse = httpClient.submitForm(
                url = "https://slack.com/api/oauth.v2.access",
                formParameters = parameters {
                    append("client_id", config.slackClientId)
                    append("client_secret", config.slackClientSecret)
                    append("code", code)
                    append("redirect_uri", redirectUri)
                }
            ).body()

            if (!tokenResponse.ok || tokenResponse.authedUser == null) {
                logger.error("Slack OAuth failed: ${tokenResponse.error}")
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "OAuth failed: ${tokenResponse.error}"))
                return@get
            }

            val slackUserId = tokenResponse.authedUser.id
            val userToken = tokenResponse.authedUser.accessToken ?: config.slackBotToken

            // Fetch user profile
            val userInfo: SlackUserInfo = httpClient.get("https://slack.com/api/users.info") {
                parameter("user", slackUserId)
                header("Authorization", "Bearer ${config.slackBotToken}")
            }.body()

            val displayName = userInfo.user?.profile?.displayName
                ?: userInfo.user?.profile?.realName
                ?: userInfo.user?.realName
                ?: slackUserId
            val avatarUrl = userInfo.user?.profile?.image192

            // Upsert player in database
            transaction {
                val existing = Players.select { Players.slackUserId eq slackUserId }.firstOrNull()
                if (existing == null) {
                    Players.insert {
                        it[Players.slackUserId] = slackUserId
                        it[Players.displayName] = displayName
                        it[Players.avatarUrl] = avatarUrl
                    }
                    logger.info("New player registered: $displayName ($slackUserId)")
                } else {
                    Players.update({ Players.slackUserId eq slackUserId }) {
                        it[Players.displayName] = displayName
                        it[Players.avatarUrl] = avatarUrl
                    }
                }
            }

            call.sessions.set(UserSession(slackUserId, displayName, avatarUrl))
            call.respondRedirect("/")
        }

        get("/me") {
            val session = call.sessions.get<UserSession>()
            if (session == null) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Not authenticated"))
                return@get
            }

            val player = transaction {
                Players.select { Players.slackUserId eq session.slackUserId }.firstOrNull()
            }

            if (player == null) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Player not found"))
                return@get
            }

            call.respond(
                mapOf(
                    "id" to player[Players.id],
                    "slackUserId" to player[Players.slackUserId],
                    "displayName" to player[Players.displayName],
                    "avatarUrl" to player[Players.avatarUrl],
                    "eloRating" to player[Players.eloRating],
                    "isAdmin" to player[Players.isAdmin],
                )
            )
        }

        post("/logout") {
            call.sessions.clear<UserSession>()
            call.respond(HttpStatusCode.OK, mapOf("status" to "logged out"))
        }
    }
}
