package com.rakket.tournament

import com.rakket.db.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.LocalDateTime

private val logger = LoggerFactory.getLogger("com.rakket.tournament.TournamentEngine")

object TournamentEngine {

    /**
     * Create a new tournament for the given date.
     */
    fun createTournament(date: LocalDate): Long {
        return transaction {
            Tournaments.insert {
                it[Tournaments.date] = date
                it[status] = "registration"
            }[Tournaments.id]
        }.also {
            logger.info("Created tournament #$it for $date")
        }
    }

    /**
     * Register a player for a tournament.
     */
    fun registerPlayer(tournamentId: Long, playerId: Long): Boolean {
        return transaction {
            val tournament = Tournaments.select { Tournaments.id eq tournamentId }.firstOrNull()
                ?: return@transaction false

            if (tournament[Tournaments.status] != "registration") return@transaction false

            val existing = TournamentParticipants.select {
                (TournamentParticipants.tournamentId eq tournamentId) and
                    (TournamentParticipants.playerId eq playerId)
            }.firstOrNull()

            if (existing != null) return@transaction false

            TournamentParticipants.insert {
                it[TournamentParticipants.tournamentId] = tournamentId
                it[TournamentParticipants.playerId] = playerId
            }

            Tournaments.update({ Tournaments.id eq tournamentId }) {
                with(SqlExpressionBuilder) {
                    it[participantCount] = participantCount + 1
                }
            }
            true
        }
    }

    /**
     * Start a tournament: close registration, generate first round pairings.
     * Requires minimum 3 players. Returns false if tournament can't start.
     */
    fun startTournament(tournamentId: Long): Boolean {
        return transaction {
            val tournament = Tournaments.select { Tournaments.id eq tournamentId }.firstOrNull()
                ?: return@transaction false

            if (tournament[Tournaments.status] != "registration") return@transaction false

            val participants = TournamentParticipants
                .select { TournamentParticipants.tournamentId eq tournamentId }
                .toList()

            if (participants.size < 3) {
                Tournaments.update({ Tournaments.id eq tournamentId }) {
                    it[status] = "cancelled"
                }
                logger.info("Tournament #$tournamentId cancelled: only ${participants.size} players")
                return@transaction false
            }

            val totalRounds = SwissPairing.calculateRounds(participants.size)

            Tournaments.update({ Tournaments.id eq tournamentId }) {
                it[status] = "active"
                it[Tournaments.totalRounds] = totalRounds
            }

            // Build player list with ELO for initial seeding
            val players = participants.map { p ->
                val player = Players.select { Players.id eq p[TournamentParticipants.playerId] }.first()
                SwissPlayer(
                    id = p[TournamentParticipants.playerId],
                    score = 0.0,
                    eloRating = player[Players.eloRating],
                )
            }

            // Generate round 1
            generateRound(tournamentId, 1, players, emptySet())

            logger.info("Tournament #$tournamentId started with ${participants.size} players, $totalRounds rounds")
            true
        }
    }

    /**
     * Generate a new round with Swiss pairings.
     */
    fun generateRound(
        tournamentId: Long,
        roundNumber: Int,
        players: List<SwissPlayer>,
        previousPairings: Set<Pair<Long, Long>>,
    ): Long {
        val roundId = TournamentRounds.insert {
            it[TournamentRounds.tournamentId] = tournamentId
            it[TournamentRounds.roundNumber] = roundNumber
            it[status] = "active"
        }[TournamentRounds.id]

        val pairings = SwissPairing.generatePairings(players, previousPairings)

        for (pairing in pairings) {
            Matches.insert {
                it[Matches.roundId] = roundId
                it[player1Id] = pairing.player1Id
                it[player2Id] = pairing.player2Id
                it[status] = if (pairing.player2Id == null) "bye" else "pending"
                it[winnerId] = if (pairing.player2Id == null) pairing.player1Id else null
                if (pairing.player2Id == null) {
                    it[completedAt] = LocalDateTime.now()
                }
            }
        }

        logger.info("Round $roundNumber generated for tournament #$tournamentId: ${pairings.size} matches")
        return roundId
    }

    /**
     * Check if a round is complete (all matches resolved) and advance to next round.
     */
    fun checkRoundCompletion(roundId: Long): Boolean {
        return transaction {
            val round = TournamentRounds.select { TournamentRounds.id eq roundId }.firstOrNull()
                ?: return@transaction false

            val pendingMatches = Matches.select {
                (Matches.roundId eq roundId) and (Matches.status eq "pending")
            }.count()

            if (pendingMatches > 0L) return@transaction false

            // Mark round as completed
            TournamentRounds.update({ TournamentRounds.id eq roundId }) {
                it[status] = "completed"
            }

            val tournamentId = round[TournamentRounds.tournamentId]
            val roundNumber = round[TournamentRounds.roundNumber]

            val tournament = Tournaments.select { Tournaments.id eq tournamentId }.first()
            val totalRounds = tournament[Tournaments.totalRounds]

            if (roundNumber >= totalRounds) {
                // Tournament is over — finalize
                finalizeTournament(tournamentId)
            } else {
                // Generate next round
                advanceToNextRound(tournamentId, roundNumber + 1)
            }

            true
        }
    }

    /**
     * Advance to the next round by computing current scores and generating new pairings.
     */
    private fun advanceToNextRound(tournamentId: Long, nextRoundNumber: Int) {
        // Collect all previous pairings
        val previousPairings = mutableSetOf<Pair<Long, Long>>()
        val rounds = TournamentRounds.select { TournamentRounds.tournamentId eq tournamentId }.toList()

        for (round in rounds) {
            val matches = Matches.select { Matches.roundId eq round[TournamentRounds.id] }.toList()
            for (match in matches) {
                val p1 = match[Matches.player1Id] ?: continue
                val p2 = match[Matches.player2Id] ?: continue
                previousPairings.add(SwissPairing.pairingKey(p1, p2))
            }
        }

        // Calculate current scores
        val participants = TournamentParticipants
            .select { TournamentParticipants.tournamentId eq tournamentId }
            .toList()

        val playerScores = mutableMapOf<Long, Double>()
        for (p in participants) {
            playerScores[p[TournamentParticipants.playerId]] = 0.0
        }

        for (round in rounds) {
            val matches = Matches.select {
                (Matches.roundId eq round[TournamentRounds.id]) and
                    (Matches.status inList listOf("completed", "bye"))
            }.toList()

            for (match in matches) {
                val winnerId = match[Matches.winnerId] ?: continue
                playerScores[winnerId] = (playerScores[winnerId] ?: 0.0) + 1.0
            }
        }

        val players = playerScores.map { (id, score) ->
            val player = Players.select { Players.id eq id }.first()
            SwissPlayer(id = id, score = score, eloRating = player[Players.eloRating])
        }

        generateRound(tournamentId, nextRoundNumber, players, previousPairings)
    }

    /**
     * Finalize a completed tournament: calculate placements and award points.
     */
    private fun finalizeTournament(tournamentId: Long) {
        // Calculate final scores
        val participants = TournamentParticipants
            .select { TournamentParticipants.tournamentId eq tournamentId }
            .toList()

        val playerScores = mutableMapOf<Long, Double>()
        for (p in participants) {
            playerScores[p[TournamentParticipants.playerId]] = 0.0
        }

        val rounds = TournamentRounds.select { TournamentRounds.tournamentId eq tournamentId }.toList()
        for (round in rounds) {
            val matches = Matches.select {
                (Matches.roundId eq round[TournamentRounds.id]) and
                    (Matches.status inList listOf("completed", "bye"))
            }.toList()

            for (match in matches) {
                val winnerId = match[Matches.winnerId] ?: continue
                playerScores[winnerId] = (playerScores[winnerId] ?: 0.0) + 1.0
            }
        }

        // Rank by score, then by ELO as tiebreaker
        val ranked = playerScores.entries
            .sortedWith(compareByDescending<Map.Entry<Long, Double>> { it.value }
                .thenByDescending { entry ->
                    Players.select { Players.id eq entry.key }.first()[Players.eloRating]
                })
            .mapIndexed { index, entry -> Triple(entry.key, index + 1, entry.value) }

        // Update placements and points
        for ((playerId, placement, _) in ranked) {
            val points = SwissPairing.calculatePoints(placement, participants.size)
            TournamentParticipants.update({
                (TournamentParticipants.tournamentId eq tournamentId) and
                    (TournamentParticipants.playerId eq playerId)
            }) {
                it[finalPlacement] = placement
                it[pointsAwarded] = points
            }
        }

        Tournaments.update({ Tournaments.id eq tournamentId }) {
            it[status] = "completed"
        }

        logger.info("Tournament #$tournamentId finalized. Winner: player #${ranked.firstOrNull()?.first}")
    }

    /**
     * Get current standings for a tournament.
     */
    fun getStandings(tournamentId: Long): List<Pair<Long, Double>> {
        return transaction {
            val playerScores = mutableMapOf<Long, Double>()

            val participants = TournamentParticipants
                .select { TournamentParticipants.tournamentId eq tournamentId }
                .toList()

            for (p in participants) {
                playerScores[p[TournamentParticipants.playerId]] = 0.0
            }

            val rounds = TournamentRounds.select { TournamentRounds.tournamentId eq tournamentId }.toList()
            for (round in rounds) {
                val matches = Matches.select {
                    (Matches.roundId eq round[TournamentRounds.id]) and
                        (Matches.status inList listOf("completed", "bye"))
                }.toList()

                for (match in matches) {
                    val winnerId = match[Matches.winnerId] ?: continue
                    playerScores[winnerId] = (playerScores[winnerId] ?: 0.0) + 1.0
                }
            }

            playerScores.entries
                .sortedByDescending { it.value }
                .map { Pair(it.key, it.value) }
        }
    }
}
