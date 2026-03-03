package com.rakket.slack

import com.rakket.config.AppConfig
import com.rakket.db.*
import com.rakket.tournament.TournamentEngine
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("com.rakket.slack.SlackBot")

class SlackBot(private val config: AppConfig) {

    private val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    /**
     * Post a message to the configured Slack channel.
     */
    suspend fun postMessage(text: String, channel: String = config.slackChannelId): String? {
        if (config.slackBotToken.isBlank()) {
            logger.warn("Slack bot token not configured, skipping message")
            return null
        }

        val response = httpClient.post("https://slack.com/api/chat.postMessage") {
            header("Authorization", "Bearer ${config.slackBotToken}")
            contentType(ContentType.Application.Json)
            setBody(mapOf(
                "channel" to channel,
                "text" to text,
                "unfurl_links" to false,
            ))
        }

        logger.info("Slack message posted to $channel")
        return null // simplified — would parse response for message_ts
    }

    /**
     * Send a DM to a user.
     */
    suspend fun sendDM(slackUserId: String, text: String) {
        if (config.slackBotToken.isBlank()) return

        httpClient.post("https://slack.com/api/chat.postMessage") {
            header("Authorization", "Bearer ${config.slackBotToken}")
            contentType(ContentType.Application.Json)
            setBody(mapOf(
                "channel" to slackUserId,
                "text" to text,
            ))
        }
    }

    /**
     * Post tournament registration message.
     */
    suspend fun postRegistrationMessage(tournamentId: Long) {
        val text = buildString {
            appendLine(":ping_pong: *It's tournament day!*")
            appendLine()
            appendLine("React with :ping_pong: to join today's tournament.")
            appendLine("Registration closes at ${config.tournamentTime}.")
            appendLine()
            appendLine("_View details at ${config.appUrl}_")
        }
        postMessage(text)
    }

    /**
     * Post round matchups to Slack.
     */
    suspend fun postRoundMatchups(tournamentId: Long, roundNumber: Int) {
        val matchups = transaction {
            val round = TournamentRounds.select {
                (TournamentRounds.tournamentId eq tournamentId) and
                    (TournamentRounds.roundNumber eq roundNumber)
            }.firstOrNull() ?: return@transaction emptyList()

            Matches.select { Matches.roundId eq round[TournamentRounds.id] }.map { match ->
                val p1Name = match[Matches.player1Id]?.let { id ->
                    Players.select { Players.id eq id }.first()[Players.displayName]
                } ?: "BYE"
                val p2Name = match[Matches.player2Id]?.let { id ->
                    Players.select { Players.id eq id }.first()[Players.displayName]
                } ?: "BYE"
                val isBye = match[Matches.status] == "bye"
                Triple(p1Name, p2Name, isBye)
            }
        }

        if (matchups.isEmpty()) return

        val text = buildString {
            appendLine(":ping_pong: *Round $roundNumber Matchups*")
            appendLine()
            for ((p1, p2, isBye) in matchups) {
                if (isBye) {
                    appendLine(":white_check_mark: *$p1* — BYE (automatic win)")
                } else {
                    appendLine(":crossed_swords: *$p1* vs *$p2*")
                }
            }
            appendLine()
            appendLine("Report scores with `/rakket score @opponent 11-7 9-11 11-5`")
        }

        postMessage(text)
    }

    /**
     * Post match result to Slack.
     */
    suspend fun postMatchResult(
        winnerName: String,
        loserName: String,
        sets: List<String>,
        winnerEloChange: Int,
        loserEloChange: Int,
    ) {
        val setsDisplay = sets.joinToString(", ")
        val trashTalk = TrashTalk.getComment(winnerName, loserName)

        val text = buildString {
            appendLine(":ping_pong: *Match Result*")
            appendLine("*$winnerName* defeated *$loserName* ($setsDisplay)")
            appendLine()
            val winSign = if (winnerEloChange >= 0) "+" else ""
            val loseSign = if (loserEloChange >= 0) "+" else ""
            appendLine("ELO: $winnerName $winSign$winnerEloChange | $loserName $loseSign$loserEloChange")
            if (trashTalk != null) {
                appendLine()
                appendLine(trashTalk)
            }
        }

        postMessage(text)
    }

    /**
     * Post tournament completion announcement.
     */
    suspend fun postTournamentResults(tournamentId: Long) {
        val results = transaction {
            TournamentParticipants
                .innerJoin(Players, { playerId }, { Players.id })
                .select { TournamentParticipants.tournamentId eq tournamentId }
                .orderBy(TournamentParticipants.finalPlacement)
                .map { row ->
                    Pair(
                        row[Players.displayName],
                        row[TournamentParticipants.finalPlacement],
                    )
                }
        }

        if (results.isEmpty()) return

        val text = buildString {
            appendLine(":trophy: *Tournament Complete!*")
            appendLine()
            for ((name, placement) in results.take(5)) {
                val medal = when (placement) {
                    1 -> ":first_place_medal:"
                    2 -> ":second_place_medal:"
                    3 -> ":third_place_medal:"
                    else -> "#$placement"
                }
                appendLine("$medal *$name*")
            }
            appendLine()
            appendLine("_Full results at ${config.appUrl}/tournaments/$tournamentId _")
        }

        postMessage(text)
    }

    /**
     * Post daily summary of yesterday's matches.
     */
    suspend fun postDailySummary() {
        val summary = transaction {
            val yesterday = java.time.LocalDateTime.now().minusDays(1)
            val matches = Matches.select {
                (Matches.status eq "completed") and
                    (Matches.completedAt greaterEq yesterday)
            }.toList()

            if (matches.isEmpty()) return@transaction null

            val matchResults = matches.map { match ->
                val winner = match[Matches.winnerId]?.let { id ->
                    Players.select { Players.id eq id }.first()[Players.displayName]
                } ?: "Unknown"
                val p1 = match[Matches.player1Id]?.let { id ->
                    Players.select { Players.id eq id }.first()[Players.displayName]
                } ?: ""
                val p2 = match[Matches.player2Id]?.let { id ->
                    Players.select { Players.id eq id }.first()[Players.displayName]
                } ?: ""
                val loser = if (winner == p1) p2 else p1

                val sets = MatchSets.select { MatchSets.matchId eq match[Matches.id] }
                    .orderBy(MatchSets.setNumber)
                    .map { "${it[MatchSets.player1Score]}-${it[MatchSets.player2Score]}" }

                Triple(winner, loser, sets.joinToString(", "))
            }

            // Biggest ELO movements
            val eloChanges = EloHistory.select {
                EloHistory.recordedAt greaterEq yesterday
            }.toList().map { row ->
                val name = Players.select { Players.id eq row[EloHistory.playerId] }.first()[Players.displayName]
                Pair(name, row[EloHistory.eloChange])
            }.sortedByDescending { kotlin.math.abs(it.second) }

            // New badges
            val newBadges = Badges.select {
                Badges.earnedAt greaterEq yesterday
            }.toList().map { row ->
                val name = Players.select { Players.id eq row[Badges.playerId] }.first()[Players.displayName]
                Pair(name, row[Badges.badgeType])
            }

            Triple(matchResults, eloChanges.take(3), newBadges)
        } ?: return

        val (matchResults, eloChanges, newBadges) = summary

        val text = buildString {
            appendLine(":newspaper: *Daily Summary*")
            appendLine()
            appendLine("*Matches (${matchResults.size}):*")
            for ((winner, loser, sets) in matchResults) {
                appendLine(":ping_pong: *$winner* def. $loser ($sets)")
            }
            if (eloChanges.isNotEmpty()) {
                appendLine()
                appendLine("*Biggest ELO moves:*")
                for ((name, change) in eloChanges) {
                    val sign = if (change >= 0) "+" else ""
                    val emoji = if (change >= 0) ":chart_with_upwards_trend:" else ":chart_with_downwards_trend:"
                    appendLine("$emoji $name: $sign$change")
                }
            }
            if (newBadges.isNotEmpty()) {
                appendLine()
                appendLine("*New badges:*")
                for ((name, badge) in newBadges) {
                    appendLine(":medal: $name earned *$badge*")
                }
            }
        }

        postMessage(text)
    }

    /**
     * Post weekly recap.
     */
    suspend fun postWeeklyRecap() {
        val recap = transaction {
            val weekAgo = java.time.LocalDateTime.now().minusDays(7)

            // Tournament winners this week
            val tournaments = Tournaments.select {
                (Tournaments.status eq "completed") and
                    (Tournaments.createdAt greaterEq weekAgo)
            }.toList()

            val winners = tournaments.mapNotNull { t ->
                TournamentParticipants.select {
                    (TournamentParticipants.tournamentId eq t[Tournaments.id]) and
                        (TournamentParticipants.finalPlacement eq 1)
                }.firstOrNull()?.let { p ->
                    Players.select { Players.id eq p[TournamentParticipants.playerId] }.first()[Players.displayName]
                }
            }

            // Biggest ELO gainer
            val biggestGainer = EloHistory.select {
                EloHistory.recordedAt greaterEq weekAgo
            }.toList()
                .groupBy { it[EloHistory.playerId] }
                .mapValues { (_, entries) -> entries.sumOf { it[EloHistory.eloChange] } }
                .maxByOrNull { it.value }
                ?.let { (playerId, change) ->
                    val name = Players.select { Players.id eq playerId }.first()[Players.displayName]
                    Pair(name, change)
                }

            // Most active player
            val mostActive = Matches.select {
                (Matches.status eq "completed") and
                    (Matches.completedAt greaterEq weekAgo)
            }.toList()
                .flatMap { listOfNotNull(it[Matches.player1Id], it[Matches.player2Id]) }
                .groupBy { it }
                .maxByOrNull { it.value.size }
                ?.let { (playerId, matches) ->
                    val name = Players.select { Players.id eq playerId }.first()[Players.displayName]
                    Pair(name, matches.size)
                }

            Triple(winners, biggestGainer, mostActive)
        }

        val (winners, biggestGainer, mostActive) = recap

        val text = buildString {
            appendLine(":calendar: *Weekly Recap*")
            appendLine()
            if (winners.isNotEmpty()) {
                appendLine("*Tournament winners:*")
                for (winner in winners) {
                    appendLine(":trophy: *$winner*")
                }
            }
            if (biggestGainer != null) {
                appendLine()
                appendLine(":chart_with_upwards_trend: *Biggest ELO gainer:* ${biggestGainer.first} (+${biggestGainer.second})")
            }
            if (mostActive != null) {
                appendLine()
                appendLine(":fire: *Most active:* ${mostActive.first} (${mostActive.second} matches)")
            }
            appendLine()
            appendLine("_Full stats at ${config.appUrl}/leaderboard _")
        }

        postMessage(text)
    }
}
