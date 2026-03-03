package com.rakket.routes

import com.rakket.db.*
import com.rakket.models.LeaderboardEntryDto
import com.rakket.models.PlayerSummaryDto
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime

fun Route.leaderboardRoutes() {
    route("/api/leaderboard") {
        get {
            val type = call.parameters["type"] ?: "elo"  // elo | points
            val period = call.parameters["period"] ?: "all"  // 4w | 12w | all

            val entries = transaction {
                when (type) {
                    "points" -> getPointsLeaderboard(period)
                    else -> getEloLeaderboard(period)
                }
            }

            call.respond(entries)
        }
    }
}

private fun getEloLeaderboard(period: String): List<LeaderboardEntryDto> {
    val players = Players.selectAll()
        .orderBy(Players.eloRating, SortOrder.DESC)
        .toList()

    return players.mapIndexed { index, row ->
        val playerId = row[Players.id]

        // Calculate ELO trend (change in last period)
        val eloTrend = when (period) {
            "4w" -> getEloTrend(playerId, 28)
            "12w" -> getEloTrend(playerId, 84)
            else -> getEloTrend(playerId, null)
        }

        val totalMatches = row[Players.totalMatches]
        val totalWins = row[Players.totalWins]

        LeaderboardEntryDto(
            rank = index + 1,
            player = PlayerSummaryDto(
                id = playerId,
                displayName = row[Players.displayName],
                avatarUrl = row[Players.avatarUrl],
                eloRating = row[Players.eloRating],
            ),
            eloRating = row[Players.eloRating],
            totalMatches = totalMatches,
            totalWins = totalWins,
            winRate = if (totalMatches > 0) totalWins.toDouble() / totalMatches else 0.0,
            eloTrend = eloTrend,
        )
    }
}

private fun getPointsLeaderboard(period: String): List<LeaderboardEntryDto> {
    val cutoff = when (period) {
        "4w" -> LocalDateTime.now().minusWeeks(4)
        "12w" -> LocalDateTime.now().minusWeeks(12)
        else -> null
    }

    // Sum points from tournament_participants joined with tournaments
    val query = TournamentParticipants
        .innerJoin(Tournaments, { tournamentId }, { Tournaments.id })
        .innerJoin(Players, { TournamentParticipants.playerId }, { Players.id })
        .slice(
            TournamentParticipants.playerId,
            Players.displayName,
            Players.avatarUrl,
            Players.eloRating,
            Players.totalMatches,
            Players.totalWins,
            TournamentParticipants.pointsAwarded.sum(),
        )
        .selectAll()

    if (cutoff != null) {
        query.andWhere { Tournaments.createdAt greaterEq cutoff }
    }

    val results = query
        .groupBy(TournamentParticipants.playerId, Players.displayName, Players.avatarUrl, Players.eloRating, Players.totalMatches, Players.totalWins)
        .orderBy(TournamentParticipants.pointsAwarded.sum(), SortOrder.DESC)
        .toList()

    return results.mapIndexed { index, row ->
        val totalMatches = row[Players.totalMatches]
        val totalWins = row[Players.totalWins]
        val points = row[TournamentParticipants.pointsAwarded.sum()] ?: 0

        LeaderboardEntryDto(
            rank = index + 1,
            player = PlayerSummaryDto(
                id = row[TournamentParticipants.playerId],
                displayName = row[Players.displayName],
                avatarUrl = row[Players.avatarUrl],
                eloRating = row[Players.eloRating],
            ),
            eloRating = points,  // Using eloRating field for points in points mode
            totalMatches = totalMatches,
            totalWins = totalWins,
            winRate = if (totalMatches > 0) totalWins.toDouble() / totalMatches else 0.0,
            eloTrend = 0,
        )
    }
}

private fun getEloTrend(playerId: Long, days: Int?): Int {
    val query = EloHistory.select { EloHistory.playerId eq playerId }

    if (days != null) {
        val cutoff = LocalDateTime.now().minusDays(days.toLong())
        query.andWhere { EloHistory.recordedAt greaterEq cutoff }
    }

    return query.sumOf { it[EloHistory.eloChange] }
}
