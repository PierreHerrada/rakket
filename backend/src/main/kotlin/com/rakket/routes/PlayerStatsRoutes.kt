package com.rakket.routes

import com.rakket.db.*
import com.rakket.models.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

fun Route.playerStatsRoutes() {
    route("/api/players") {
        get("/{id}/stats") {
            val id = call.parameters["id"]?.toLongOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid player ID"))
                return@get
            }

            val stats = transaction {
                val player = Players.select { Players.id eq id }.firstOrNull()?.toPlayerDto()
                    ?: return@transaction null

                // Tournaments
                val participations = TournamentParticipants
                    .select { TournamentParticipants.playerId eq id }
                    .toList()

                val tournamentsPlayed = participations.size
                val tournamentWins = participations.count { it[TournamentParticipants.finalPlacement] == 1 }
                val placements = participations.mapNotNull { it[TournamentParticipants.finalPlacement] }
                val bestPlacement = placements.minOrNull()
                val avgPlacement = if (placements.isNotEmpty()) placements.average() else null

                // Win streaks
                val allMatches = Matches
                    .select {
                        ((Matches.player1Id eq id) or (Matches.player2Id eq id)) and
                            (Matches.status eq "completed")
                    }
                    .orderBy(Matches.completedAt, SortOrder.ASC)
                    .toList()

                var currentStreak = 0
                var longestStreak = 0
                for (match in allMatches) {
                    if (match[Matches.winnerId] == id) {
                        currentStreak++
                        longestStreak = maxOf(longestStreak, currentStreak)
                    } else {
                        currentStreak = 0
                    }
                }

                // Recent matches (last 10)
                val recentMatches = allMatches.takeLast(10).reversed().map { m ->
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

                // Head-to-head
                val opponentStats = mutableMapOf<Long, Pair<Int, Int>>()
                for (match in allMatches) {
                    val opponentId = if (match[Matches.player1Id] == id) {
                        match[Matches.player2Id]
                    } else {
                        match[Matches.player1Id]
                    } ?: continue

                    val (wins, losses) = opponentStats.getOrDefault(opponentId, Pair(0, 0))
                    if (match[Matches.winnerId] == id) {
                        opponentStats[opponentId] = Pair(wins + 1, losses)
                    } else {
                        opponentStats[opponentId] = Pair(wins, losses + 1)
                    }
                }

                val h2h = opponentStats.map { (oppId, record) ->
                    HeadToHeadDto(
                        opponent = findPlayerSummary(oppId) ?: PlayerSummaryDto(oppId, "Unknown"),
                        wins = record.first,
                        losses = record.second,
                        totalMatches = record.first + record.second,
                    )
                }.sortedByDescending { it.totalMatches }

                // Badges
                val badges = Badges.select { Badges.playerId eq id }
                    .orderBy(Badges.earnedAt, SortOrder.DESC)
                    .map { row ->
                        val badgeType = row[Badges.badgeType]
                        val badgeInfo = BadgeDefinitions.get(badgeType)
                        BadgeDto(
                            badgeType = badgeType,
                            name = badgeInfo?.name ?: badgeType,
                            description = badgeInfo?.description ?: "",
                            emoji = badgeInfo?.emoji ?: "",
                            earnedAt = row[Badges.earnedAt].toString(),
                        )
                    }

                PlayerStatsDto(
                    player = player,
                    tournamentsPlayed = tournamentsPlayed,
                    tournamentWins = tournamentWins,
                    bestPlacement = bestPlacement,
                    averagePlacement = avgPlacement,
                    currentWinStreak = currentStreak,
                    longestWinStreak = longestStreak,
                    badges = badges,
                    recentMatches = recentMatches,
                    headToHead = h2h,
                )
            }

            if (stats == null) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Player not found"))
                return@get
            }

            call.respond(stats)
        }
    }
}
