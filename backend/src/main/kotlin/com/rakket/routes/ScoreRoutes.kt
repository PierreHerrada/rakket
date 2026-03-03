package com.rakket.routes

import com.rakket.db.*
import com.rakket.models.ScoreReportRequest
import com.rakket.tournament.ScoreValidator
import com.rakket.tournament.TournamentEngine
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

private val logger = LoggerFactory.getLogger("com.rakket.routes.ScoreRoutes")

fun Route.scoreRoutes() {
    route("/api/tournaments") {
        post("/{id}/score") {
            val session = call.sessions.get<UserSession>()
            if (session == null) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Not authenticated"))
                return@post
            }

            val request = call.receive<ScoreReportRequest>()

            val result = transaction {
                val match = Matches.select { Matches.id eq request.matchId }.firstOrNull()
                    ?: return@transaction Pair(HttpStatusCode.NotFound, "Match not found")

                if (match[Matches.status] != "pending") {
                    return@transaction Pair(HttpStatusCode.BadRequest, "Match is not pending")
                }

                val player1Id = match[Matches.player1Id]
                    ?: return@transaction Pair(HttpStatusCode.BadRequest, "Invalid match: missing player 1")
                val player2Id = match[Matches.player2Id]
                    ?: return@transaction Pair(HttpStatusCode.BadRequest, "Invalid match: missing player 2")

                // Verify reporter is one of the players
                val reporter = Players.select { Players.slackUserId eq session.slackUserId }
                    .firstOrNull()
                    ?: return@transaction Pair(HttpStatusCode.Forbidden, "Player not found")

                val reporterId = reporter[Players.id]
                if (reporterId != player1Id && reporterId != player2Id) {
                    return@transaction Pair(HttpStatusCode.Forbidden, "You are not a participant in this match")
                }

                // Validate score
                val validation = ScoreValidator.validateBestOf3(request.sets, player1Id, player2Id)
                if (!validation.valid) {
                    return@transaction Pair(HttpStatusCode.BadRequest, validation.error ?: "Invalid score")
                }

                val winnerId = validation.winnerId!!

                // Save set scores
                for (set in request.sets) {
                    MatchSets.insert {
                        it[matchId] = request.matchId
                        it[setNumber] = set.setNumber
                        it[player1Score] = set.player1Score
                        it[player2Score] = set.player2Score
                    }
                }

                // Update match
                Matches.update({ Matches.id eq request.matchId }) {
                    it[Matches.winnerId] = winnerId
                    it[status] = "completed"
                    it[Matches.reportedBy] = reporterId
                    it[completedAt] = LocalDateTime.now()
                }

                // Update player stats
                Players.update({ Players.id eq player1Id }) {
                    with(SqlExpressionBuilder) {
                        it[totalMatches] = totalMatches + 1
                        if (winnerId == player1Id) {
                            it[totalWins] = totalWins + 1
                        }
                    }
                }
                Players.update({ Players.id eq player2Id }) {
                    with(SqlExpressionBuilder) {
                        it[totalMatches] = totalMatches + 1
                        if (winnerId == player2Id) {
                            it[totalWins] = totalWins + 1
                        }
                    }
                }

                logger.info("Score reported for match #${request.matchId}: winner=$winnerId by reporter=$reporterId")

                // Check if the round is complete
                val roundId = match[Matches.roundId]
                TournamentEngine.checkRoundCompletion(roundId)

                Pair(HttpStatusCode.OK, "Score recorded successfully")
            }

            call.respond(result.first, mapOf("message" to result.second))
        }
    }
}
