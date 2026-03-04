package com.rakket.routes

import com.rakket.config.AppConfig
import com.rakket.db.*
import com.rakket.elo.EloCalculator
import com.rakket.models.ScoreReportRequest
import com.rakket.tournament.ScoreValidator
import com.rakket.tournament.TournamentEngine
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.LocalDateTime

private val logger = LoggerFactory.getLogger("com.rakket.routes.AdminRoutes")

fun Route.adminRoutes(config: AppConfig) {
    route("/api/admin") {
        // Middleware: check admin status
        intercept(io.ktor.server.application.ApplicationCallPipeline.Call) {
            val session = call.sessions.get<UserSession>()
            if (session == null) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Not authenticated"))
                finish()
                return@intercept
            }
            val isAdmin = transaction {
                Players.select { Players.slackUserId eq session.slackUserId }
                    .firstOrNull()
                    ?.get(Players.isAdmin) ?: false
            }
            if (!isAdmin) {
                call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Admin access required"))
                finish()
                return@intercept
            }
        }

        post("/tournament/create") {
            val date = LocalDate.now()
            val tournamentId = TournamentEngine.createTournament(date)
            call.respond(HttpStatusCode.Created, mapOf("id" to tournamentId, "message" to "Tournament created"))
        }

        put("/match/{id}/score") {
            val matchId = call.parameters["id"]?.toLongOrNull()
            if (matchId == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid match ID"))
                return@put
            }

            val request = call.receive<ScoreReportRequest>()

            val result = transaction {
                val match = Matches.select { Matches.id eq matchId }.firstOrNull()
                    ?: return@transaction Pair(HttpStatusCode.NotFound, "Match not found")

                val player1Id = match[Matches.player1Id]
                    ?: return@transaction Pair(HttpStatusCode.BadRequest, "Invalid match")
                val player2Id = match[Matches.player2Id]
                    ?: return@transaction Pair(HttpStatusCode.BadRequest, "Invalid match")

                val validation = ScoreValidator.validateBestOf3(request.sets, player1Id, player2Id)
                if (!validation.valid) {
                    return@transaction Pair(HttpStatusCode.BadRequest, validation.error ?: "Invalid score")
                }

                // Delete old sets if overriding
                MatchSets.deleteWhere { MatchSets.matchId eq matchId }

                // Insert new sets
                for (set in request.sets) {
                    MatchSets.insert {
                        it[MatchSets.matchId] = matchId
                        it[setNumber] = set.setNumber
                        it[player1Score] = set.player1Score
                        it[player2Score] = set.player2Score
                    }
                }

                Matches.update({ Matches.id eq matchId }) {
                    it[winnerId] = validation.winnerId
                    it[status] = "completed"
                    it[completedAt] = LocalDateTime.now()
                }

                logger.info("Admin override: match #$matchId score updated, winner=${validation.winnerId}")
                Pair(HttpStatusCode.OK, "Score overridden successfully")
            }

            call.respond(result.first, mapOf("message" to result.second))
        }

        put("/match/{id}/dispute") {
            val matchId = call.parameters["id"]?.toLongOrNull()
            if (matchId == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid match ID"))
                return@put
            }

            @Serializable
            data class DisputeResolution(val action: String) // "resolve" or "reset"

            val body = call.receive<DisputeResolution>()

            transaction {
                when (body.action) {
                    "resolve" -> {
                        Matches.update({ Matches.id eq matchId }) {
                            it[status] = "completed"
                        }
                    }
                    "reset" -> {
                        MatchSets.deleteWhere { MatchSets.matchId eq matchId }
                        Matches.update({ Matches.id eq matchId }) {
                            it[winnerId] = null
                            it[status] = "pending"
                            it[completedAt] = null
                            it[reportedBy] = null
                        }
                    }
                    else -> {
                        logger.warn("Unknown dispute action: ${body.action}")
                    }
                }
            }

            call.respond(HttpStatusCode.OK, mapOf("message" to "Dispute ${body.action}d for match #$matchId"))
        }

        get("/settings") {
            call.respond(mapOf(
                "appUrl" to config.appUrl,
                "timezone" to config.timezone,
                "tournamentDay" to config.tournamentDay,
                "registrationTime" to config.registrationTime,
                "tournamentTime" to config.tournamentTime,
                "matchFormat" to config.matchFormat,
            ))
        }
    }
}
