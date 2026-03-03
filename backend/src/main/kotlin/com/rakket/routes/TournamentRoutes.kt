package com.rakket.routes

import com.rakket.db.*
import com.rakket.models.*
import com.rakket.tournament.TournamentEngine
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

fun Route.tournamentRoutes() {
    route("/api/tournaments") {
        get {
            val page = call.parameters["page"]?.toIntOrNull() ?: 0
            val limit = (call.parameters["limit"]?.toIntOrNull() ?: 20).coerceIn(1, 50)

            val tournaments = transaction {
                Tournaments.selectAll()
                    .orderBy(Tournaments.date, SortOrder.DESC)
                    .limit(limit, (page * limit).toLong())
                    .map { row ->
                        TournamentDto(
                            id = row[Tournaments.id],
                            date = row[Tournaments.date].toString(),
                            status = row[Tournaments.status],
                            participantCount = row[Tournaments.participantCount],
                            totalRounds = row[Tournaments.totalRounds],
                            createdAt = row[Tournaments.createdAt].toString(),
                        )
                    }
            }

            call.respond(tournaments)
        }

        get("/current") {
            val tournament = transaction {
                Tournaments.select {
                    Tournaments.status inList listOf("registration", "active")
                }
                    .orderBy(Tournaments.date, SortOrder.DESC)
                    .firstOrNull()
                    ?.let { buildTournamentDetail(it) }
            }

            if (tournament == null) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "No active tournament"))
                return@get
            }

            call.respond(tournament)
        }

        get("/{id}") {
            val id = call.parameters["id"]?.toLongOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid tournament ID"))
                return@get
            }

            val tournament = transaction {
                Tournaments.select { Tournaments.id eq id }
                    .firstOrNull()
                    ?.let { buildTournamentDetail(it) }
            }

            if (tournament == null) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Tournament not found"))
                return@get
            }

            call.respond(tournament)
        }
    }
}

private fun buildTournamentDetail(row: ResultRow): TournamentDetailDto {
    val tournamentId = row[Tournaments.id]

    val participants = TournamentParticipants
        .select { TournamentParticipants.tournamentId eq tournamentId }
        .map { p ->
            val player = Players.select { Players.id eq p[TournamentParticipants.playerId] }.first()
            ParticipantDto(
                playerId = p[TournamentParticipants.playerId],
                displayName = player[Players.displayName],
                avatarUrl = player[Players.avatarUrl],
                finalPlacement = p[TournamentParticipants.finalPlacement],
                pointsAwarded = p[TournamentParticipants.pointsAwarded],
                roundsWon = p[TournamentParticipants.roundsWon],
            )
        }

    val rounds = TournamentRounds
        .select { TournamentRounds.tournamentId eq tournamentId }
        .orderBy(TournamentRounds.roundNumber)
        .map { r ->
            val matches = Matches
                .select { Matches.roundId eq r[TournamentRounds.id] }
                .map { m ->
                    val sets = MatchSets.select { MatchSets.matchId eq m[Matches.id] }
                        .orderBy(MatchSets.setNumber)
                        .map { s ->
                            SetScoreDto(
                                setNumber = s[MatchSets.setNumber],
                                player1Score = s[MatchSets.player1Score],
                                player2Score = s[MatchSets.player2Score],
                            )
                        }

                    MatchDto(
                        id = m[Matches.id],
                        player1 = m[Matches.player1Id]?.let { findPlayerSummary(it) },
                        player2 = m[Matches.player2Id]?.let { findPlayerSummary(it) },
                        winner = m[Matches.winnerId]?.let { findPlayerSummary(it) },
                        status = m[Matches.status],
                        sets = sets,
                        completedAt = m[Matches.completedAt]?.toString(),
                    )
                }

            RoundDto(
                id = r[TournamentRounds.id],
                roundNumber = r[TournamentRounds.roundNumber],
                status = r[TournamentRounds.status],
                matches = matches,
            )
        }

    return TournamentDetailDto(
        tournament = TournamentDto(
            id = tournamentId,
            date = row[Tournaments.date].toString(),
            status = row[Tournaments.status],
            participantCount = row[Tournaments.participantCount],
            totalRounds = row[Tournaments.totalRounds],
            createdAt = row[Tournaments.createdAt].toString(),
        ),
        participants = participants,
        rounds = rounds,
    )
}
