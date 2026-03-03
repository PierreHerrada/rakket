package com.rakket.slack

import com.rakket.config.AppConfig
import com.rakket.db.*
import com.rakket.elo.EloCalculator
import com.rakket.models.SetScoreDto
import com.rakket.tournament.ScoreValidator
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

private val logger = LoggerFactory.getLogger("com.rakket.slack.SlackCommands")

/**
 * Handles Slack slash commands: /rakket score|standings|leaderboard|profile|next|history|help|stats
 */
fun Route.slackCommandRoutes(config: AppConfig) {
    post("/api/slack/commands") {
        val params = call.receiveParameters()
        val command = params["text"]?.trim() ?: ""
        val userId = params["user_id"] ?: ""
        val parts = command.split("\\s+".toRegex())

        val response = when (parts.firstOrNull()?.lowercase()) {
            "score" -> handleScore(parts, userId)
            "standings" -> handleStandings()
            "leaderboard" -> handleLeaderboard()
            "profile" -> handleProfile(parts)
            "next" -> handleNext(userId)
            "history" -> handleHistory(config)
            "stats" -> handleStats(userId)
            "help" -> handleHelp()
            else -> handleHelp()
        }

        call.respondText(response, ContentType.Text.Plain)
    }
}

private fun handleScore(parts: List<String>, reporterId: String): String {
    // /rakket score @opponent 11-7 9-11 11-5
    if (parts.size < 3) {
        return "Usage: /rakket score @opponent 11-7 9-11 11-5"
    }

    val opponentMention = parts[1].replace(Regex("[<@>]"), "")
    val setScores = parts.drop(2).mapIndexedNotNull { index, s ->
        val scoreParts = s.split("-")
        if (scoreParts.size == 2) {
            val p1 = scoreParts[0].toIntOrNull()
            val p2 = scoreParts[1].toIntOrNull()
            if (p1 != null && p2 != null) SetScoreDto(index + 1, p1, p2) else null
        } else null
    }

    if (setScores.isEmpty()) {
        return "Invalid scores. Format: 11-7 9-11 11-5"
    }

    return transaction {
        val reporter = Players.select { Players.slackUserId eq reporterId }.firstOrNull()
            ?: return@transaction "You are not registered. Log in at the web UI first."

        val opponent = Players.select { Players.slackUserId eq opponentMention }.firstOrNull()
            ?: return@transaction "Opponent not found. Make sure they're registered."

        val reporterPlayerId = reporter[Players.id]
        val opponentPlayerId = opponent[Players.id]

        // Find the pending match between these two
        val match = Matches.select {
            (Matches.status eq "pending") and
                (((Matches.player1Id eq reporterPlayerId) and (Matches.player2Id eq opponentPlayerId)) or
                    ((Matches.player1Id eq opponentPlayerId) and (Matches.player2Id eq reporterPlayerId)))
        }.firstOrNull()
            ?: return@transaction "No pending match found between you and ${opponent[Players.displayName]}."

        val p1Id = match[Matches.player1Id]!!
        val p2Id = match[Matches.player2Id]!!

        val validation = ScoreValidator.validateBestOf3(setScores, p1Id, p2Id)
        if (!validation.valid) {
            return@transaction "Invalid score: ${validation.error}"
        }

        val winnerId = validation.winnerId!!
        val winnerName = Players.select { Players.id eq winnerId }.first()[Players.displayName]

        // Record sets and update match
        for (set in setScores) {
            MatchSets.insert {
                it[matchId] = match[Matches.id]
                it[setNumber] = set.setNumber
                it[player1Score] = set.player1Score
                it[player2Score] = set.player2Score
            }
        }

        Matches.update({ Matches.id eq match[Matches.id] }) {
            it[Matches.winnerId] = winnerId
            it[status] = "completed"
            it[reportedBy] = reporterPlayerId
            it[completedAt] = LocalDateTime.now()
        }

        // Update stats
        for (pid in listOf(p1Id, p2Id)) {
            Players.update({ Players.id eq pid }) {
                with(SqlExpressionBuilder) {
                    it[totalMatches] = totalMatches + 1
                    if (winnerId == pid) {
                        it[totalWins] = totalWins + 1
                    }
                }
            }
        }

        EloCalculator.applyMatchResult(match[Matches.id], p1Id, p2Id, winnerId)

        ":white_check_mark: Score recorded! *$winnerName* wins. Sets: ${setScores.joinToString(", ") { "${it.player1Score}-${it.player2Score}" }}"
    }
}

private fun handleStandings(): String {
    return transaction {
        val tournament = Tournaments.select {
            Tournaments.status inList listOf("active", "registration")
        }.orderBy(Tournaments.date, SortOrder.DESC).firstOrNull()
            ?: return@transaction "No active tournament."

        val standings = com.rakket.tournament.TournamentEngine.getStandings(tournament[Tournaments.id])

        if (standings.isEmpty()) return@transaction "No standings yet."

        buildString {
            appendLine(":ping_pong: *Current Standings*")
            for ((index, entry) in standings.withIndex()) {
                val name = Players.select { Players.id eq entry.first }.first()[Players.displayName]
                appendLine("${index + 1}. *$name* — ${entry.second.toInt()} wins")
            }
        }
    }
}

private fun handleLeaderboard(): String {
    return transaction {
        val top10 = Players.selectAll()
            .orderBy(Players.eloRating, SortOrder.DESC)
            .limit(10)
            .toList()

        if (top10.isEmpty()) return@transaction "No players yet."

        buildString {
            appendLine(":trophy: *ELO Leaderboard (Top 10)*")
            for ((index, player) in top10.withIndex()) {
                val medal = when (index) {
                    0 -> ":first_place_medal:"
                    1 -> ":second_place_medal:"
                    2 -> ":third_place_medal:"
                    else -> "${index + 1}."
                }
                appendLine("$medal *${player[Players.displayName]}* — ${player[Players.eloRating]} ELO")
            }
        }
    }
}

private fun handleProfile(parts: List<String>): String {
    val targetUserId = if (parts.size >= 2) {
        parts[1].replace(Regex("[<@>]"), "")
    } else {
        return "Usage: /rakket profile @user"
    }

    return transaction {
        val player = Players.select { Players.slackUserId eq targetUserId }.firstOrNull()
            ?: return@transaction "Player not found."

        val winRate = if (player[Players.totalMatches] > 0) {
            ((player[Players.totalWins].toDouble() / player[Players.totalMatches]) * 100).toInt()
        } else 0

        buildString {
            appendLine(":bust_in_silhouette: *${player[Players.displayName]}*")
            appendLine("ELO: ${player[Players.eloRating]} | Matches: ${player[Players.totalMatches]} | Win rate: $winRate%")
        }
    }
}

private fun handleNext(userId: String): String {
    return transaction {
        val player = Players.select { Players.slackUserId eq userId }.firstOrNull()
            ?: return@transaction "You are not registered."

        val playerId = player[Players.id]

        val nextMatch = Matches.select {
            (Matches.status eq "pending") and
                ((Matches.player1Id eq playerId) or (Matches.player2Id eq playerId))
        }.firstOrNull()

        if (nextMatch == null) {
            "You have no pending matches."
        } else {
            val opponentId = if (nextMatch[Matches.player1Id] == playerId) {
                nextMatch[Matches.player2Id]
            } else {
                nextMatch[Matches.player1Id]
            }
            val opponentName = opponentId?.let { id ->
                Players.select { Players.id eq id }.first()[Players.displayName]
            } ?: "BYE"

            ":ping_pong: Your next match: vs *$opponentName*"
        }
    }
}

private fun handleHistory(config: AppConfig): String {
    return ":books: View full tournament history at ${config.appUrl}/tournaments"
}

private fun handleStats(userId: String): String {
    return transaction {
        val player = Players.select { Players.slackUserId eq userId }.firstOrNull()
            ?: return@transaction "You are not registered."

        val winRate = if (player[Players.totalMatches] > 0) {
            ((player[Players.totalWins].toDouble() / player[Players.totalMatches]) * 100).toInt()
        } else 0

        val tournamentCount = TournamentParticipants
            .select { TournamentParticipants.playerId eq player[Players.id] }
            .count()

        val badges = Badges.select { Badges.playerId eq player[Players.id] }.count()

        buildString {
            appendLine(":bar_chart: *Your Stats*")
            appendLine("ELO: ${player[Players.eloRating]}")
            appendLine("Matches: ${player[Players.totalMatches]} (${player[Players.totalWins]}W)")
            appendLine("Win rate: $winRate%")
            appendLine("Tournaments: $tournamentCount")
            appendLine("Badges: $badges")
        }
    }
}

private fun handleHelp(): String {
    return buildString {
        appendLine(":ping_pong: *Rakket Commands*")
        appendLine()
        appendLine("`/rakket score @opponent 11-7 9-11 11-5` — Report a match score")
        appendLine("`/rakket standings` — Current tournament standings")
        appendLine("`/rakket leaderboard` — Top 10 ELO rankings")
        appendLine("`/rakket profile @user` — Player stats summary")
        appendLine("`/rakket next` — Your next match")
        appendLine("`/rakket history` — Tournament history link")
        appendLine("`/rakket stats` — Your personal stats")
        appendLine("`/rakket help` — Show this help message")
    }
}
