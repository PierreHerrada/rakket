package com.rakket.routes

import com.rakket.db.*
import com.rakket.models.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

fun Route.playerRoutes() {
    route("/api/players") {
        get {
            val players = transaction {
                Players.selectAll()
                    .orderBy(Players.eloRating, SortOrder.DESC)
                    .map { it.toPlayerDto() }
            }
            call.respond(players)
        }

        get("/{id}") {
            val id = call.parameters["id"]?.toLongOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid player ID"))
                return@get
            }

            val player = transaction {
                Players.select { Players.id eq id }.firstOrNull()?.toPlayerDto()
            }

            if (player == null) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Player not found"))
                return@get
            }

            call.respond(player)
        }

        get("/{id}/matches") {
            val id = call.parameters["id"]?.toLongOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid player ID"))
                return@get
            }

            val page = call.parameters["page"]?.toIntOrNull() ?: 0
            val limit = (call.parameters["limit"]?.toIntOrNull() ?: 20).coerceIn(1, 50)

            val matches = transaction {
                Matches.select {
                    (Matches.player1Id eq id) or (Matches.player2Id eq id)
                }
                    .orderBy(Matches.createdAt, SortOrder.DESC)
                    .limit(limit, (page * limit).toLong())
                    .map { row ->
                        val sets = MatchSets.select { MatchSets.matchId eq row[Matches.id] }
                            .orderBy(MatchSets.setNumber)
                            .map { s ->
                                SetScoreDto(
                                    setNumber = s[MatchSets.setNumber],
                                    player1Score = s[MatchSets.player1Score],
                                    player2Score = s[MatchSets.player2Score],
                                )
                            }

                        MatchDto(
                            id = row[Matches.id],
                            player1 = row[Matches.player1Id]?.let { findPlayerSummary(it) },
                            player2 = row[Matches.player2Id]?.let { findPlayerSummary(it) },
                            winner = row[Matches.winnerId]?.let { findPlayerSummary(it) },
                            status = row[Matches.status],
                            sets = sets,
                            completedAt = row[Matches.completedAt]?.toString(),
                        )
                    }
            }

            call.respond(matches)
        }

        get("/{id}/h2h") {
            val id = call.parameters["id"]?.toLongOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid player ID"))
                return@get
            }

            val h2h = transaction {
                // Find all completed matches for this player
                val playerMatches = Matches.select {
                    ((Matches.player1Id eq id) or (Matches.player2Id eq id)) and
                        (Matches.status eq "completed")
                }.toList()

                // Group by opponent
                val opponentStats = mutableMapOf<Long, Pair<Int, Int>>() // opponentId -> (wins, losses)
                for (match in playerMatches) {
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

                opponentStats.map { (opponentId, record) ->
                    HeadToHeadDto(
                        opponent = findPlayerSummary(opponentId) ?: PlayerSummaryDto(opponentId, "Unknown"),
                        wins = record.first,
                        losses = record.second,
                        totalMatches = record.first + record.second,
                    )
                }.sortedByDescending { it.totalMatches }
            }

            call.respond(h2h)
        }

        get("/{id}/badges") {
            val id = call.parameters["id"]?.toLongOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid player ID"))
                return@get
            }

            val badges = transaction {
                Badges.select { Badges.playerId eq id }
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
            }

            call.respond(badges)
        }

        get("/{id}/elo-history") {
            val id = call.parameters["id"]?.toLongOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid player ID"))
                return@get
            }

            val history = transaction {
                EloHistory.select { EloHistory.playerId eq id }
                    .orderBy(EloHistory.recordedAt, SortOrder.ASC)
                    .map { row ->
                        EloHistoryDto(
                            id = row[EloHistory.id],
                            playerId = row[EloHistory.playerId],
                            matchId = row[EloHistory.matchId],
                            eloBefore = row[EloHistory.eloBefore],
                            eloAfter = row[EloHistory.eloAfter],
                            eloChange = row[EloHistory.eloChange],
                            recordedAt = row[EloHistory.recordedAt].toString(),
                        )
                    }
            }

            call.respond(history)
        }
    }
}

object BadgeDefinitions {
    data class BadgeInfo(val name: String, val description: String, val emoji: String)

    private val badges = mapOf(
        "CHAMPION" to BadgeInfo("Champion", "Win a tournament", "\uD83C\uDFC6"),
        "ON_FIRE" to BadgeInfo("On Fire", "Win 5 matches in a row", "\uD83D\uDD25"),
        "THE_WALL" to BadgeInfo("The Wall", "Win a set 11-0", "\uD83E\uDDF1"),
        "SNIPER" to BadgeInfo("Sniper", "Win 10 tournaments", "\uD83C\uDFAF"),
        "FIRST_BLOOD" to BadgeInfo("First Blood", "Win your first ever match", "\uD83D\uDC23"),
        "GIANT_KILLER" to BadgeInfo("Giant Killer", "Beat a player with 200+ higher ELO", "\uD83D\uDCAA"),
        "RISING_STAR" to BadgeInfo("Rising Star", "Gain 100+ ELO in a single week", "\uD83D\uDCC8"),
        "PERFECT_GAME" to BadgeInfo("Perfect Game", "Win a tournament without dropping a set", "\uD83C\uDFB3"),
        "RIVAL" to BadgeInfo("Rival", "Play the same opponent 10+ times", "\uD83E\uDD1D"),
        "CONSISTENT" to BadgeInfo("Consistent", "Participate in 10 consecutive tournaments", "\uD83C\uDFC5"),
        "COMEBACK_KID" to BadgeInfo("Comeback Kid", "Win a match after losing the first set", "\uD83D\uDD04"),
        "UNDISPUTED" to BadgeInfo("Undisputed", "Win 3 tournaments in a row", "\uD83D\uDC51"),
        "VETERAN" to BadgeInfo("Veteran", "Play 100+ matches", "\uD83C\uDF82"),
        "DEBUTANT" to BadgeInfo("Debutant", "Play your first tournament", "\uD83C\uDF1F"),
        "SOCIAL_BUTTERFLY" to BadgeInfo("Social Butterfly", "Play against 20+ different opponents", "\uD83C\uDFAA"),
    )

    fun get(type: String): BadgeInfo? = badges[type]
    fun all(): Map<String, BadgeInfo> = badges
}

fun ResultRow.toPlayerDto(): PlayerDto {
    return PlayerDto(
        id = this[Players.id],
        slackUserId = this[Players.slackUserId],
        displayName = this[Players.displayName],
        avatarUrl = this[Players.avatarUrl],
        eloRating = this[Players.eloRating],
        totalMatches = this[Players.totalMatches],
        totalWins = this[Players.totalWins],
        isAdmin = this[Players.isAdmin],
        createdAt = this[Players.createdAt].toString(),
        updatedAt = this[Players.updatedAt].toString(),
    )
}

fun findPlayerSummary(playerId: Long): PlayerSummaryDto? {
    return Players.select { Players.id eq playerId }.firstOrNull()?.let { row ->
        PlayerSummaryDto(
            id = row[Players.id],
            displayName = row[Players.displayName],
            avatarUrl = row[Players.avatarUrl],
            eloRating = row[Players.eloRating],
        )
    }
}
