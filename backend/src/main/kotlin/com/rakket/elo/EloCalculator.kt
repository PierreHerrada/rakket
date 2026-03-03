package com.rakket.elo

import com.rakket.db.EloHistory
import com.rakket.db.Players
import org.jetbrains.exposed.sql.SqlExpressionBuilder
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.update
import org.slf4j.LoggerFactory
import kotlin.math.pow
import kotlin.math.roundToInt

private val logger = LoggerFactory.getLogger("com.rakket.elo.EloCalculator")

data class EloResult(
    val player1NewRating: Int,
    val player2NewRating: Int,
    val player1Change: Int,
    val player2Change: Int,
)

object EloCalculator {

    private const val NEW_PLAYER_THRESHOLD = 20
    private const val K_NEW = 32
    private const val K_ESTABLISHED = 16

    /**
     * Calculate new ELO ratings after a match.
     *
     * @param player1Rating Current ELO of player 1
     * @param player2Rating Current ELO of player 2
     * @param player1Won True if player 1 won
     * @param player1Matches Total matches played by player 1 (for K-factor)
     * @param player2Matches Total matches played by player 2 (for K-factor)
     */
    fun calculate(
        player1Rating: Int,
        player2Rating: Int,
        player1Won: Boolean,
        player1Matches: Int = 0,
        player2Matches: Int = 0,
    ): EloResult {
        val k1 = if (player1Matches < NEW_PLAYER_THRESHOLD) K_NEW else K_ESTABLISHED
        val k2 = if (player2Matches < NEW_PLAYER_THRESHOLD) K_NEW else K_ESTABLISHED

        // Expected score for player 1
        val expected1 = expectedScore(player1Rating, player2Rating)
        val expected2 = 1.0 - expected1

        // Actual score
        val actual1 = if (player1Won) 1.0 else 0.0
        val actual2 = 1.0 - actual1

        // Calculate changes
        val change1 = (k1 * (actual1 - expected1)).roundToInt()
        val change2 = (k2 * (actual2 - expected2)).roundToInt()

        return EloResult(
            player1NewRating = player1Rating + change1,
            player2NewRating = player2Rating + change2,
            player1Change = change1,
            player2Change = change2,
        )
    }

    /**
     * Calculate expected score using the standard ELO formula.
     * Returns a value between 0 and 1.
     */
    fun expectedScore(rating1: Int, rating2: Int): Double {
        return 1.0 / (1.0 + 10.0.pow((rating2 - rating1) / 400.0))
    }

    /**
     * Apply ELO changes to players after a match.
     * Updates player ratings and records history.
     */
    fun applyMatchResult(matchId: Long, player1Id: Long, player2Id: Long, winnerId: Long) {
        val player1 = Players.select { Players.id eq player1Id }.first()
        val player2 = Players.select { Players.id eq player2Id }.first()

        val p1Rating = player1[Players.eloRating]
        val p2Rating = player2[Players.eloRating]
        val p1Matches = player1[Players.totalMatches]
        val p2Matches = player2[Players.totalMatches]

        val result = calculate(
            player1Rating = p1Rating,
            player2Rating = p2Rating,
            player1Won = winnerId == player1Id,
            player1Matches = p1Matches,
            player2Matches = p2Matches,
        )

        // Update ratings
        Players.update({ Players.id eq player1Id }) {
            it[eloRating] = result.player1NewRating
        }
        Players.update({ Players.id eq player2Id }) {
            it[eloRating] = result.player2NewRating
        }

        // Record history
        EloHistory.insert {
            it[playerId] = player1Id
            it[EloHistory.matchId] = matchId
            it[eloBefore] = p1Rating
            it[eloAfter] = result.player1NewRating
            it[eloChange] = result.player1Change
        }
        EloHistory.insert {
            it[playerId] = player2Id
            it[EloHistory.matchId] = matchId
            it[eloBefore] = p2Rating
            it[eloAfter] = result.player2NewRating
            it[eloChange] = result.player2Change
        }

        logger.info(
            "ELO updated: player #$player1Id ${p1Rating}->${result.player1NewRating} (${result.player1Change}), " +
                "player #$player2Id ${p2Rating}->${result.player2NewRating} (${result.player2Change})"
        )
    }
}
