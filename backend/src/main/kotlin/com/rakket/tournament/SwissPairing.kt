package com.rakket.tournament

import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("com.rakket.tournament.SwissPairing")

data class SwissPlayer(
    val id: Long,
    val score: Double = 0.0,
    val eloRating: Int = 1000,
)

data class Pairing(
    val player1Id: Long,
    val player2Id: Long?,  // null means bye
)

/**
 * Swiss-system tournament pairing using the Dutch system variant.
 *
 * Rules:
 * - Sort players by score (descending), then by ELO as tiebreaker
 * - Pair top player with middle player within score groups
 * - No rematches within the same tournament
 * - Odd number of players: lowest-scoring unpaired player gets a bye
 */
object SwissPairing {

    /**
     * Calculate optimal number of rounds for n players: ceil(log2(n))
     */
    fun calculateRounds(playerCount: Int): Int {
        if (playerCount <= 1) return 0
        return Math.ceil(Math.log(playerCount.toDouble()) / Math.log(2.0)).toInt()
    }

    /**
     * Generate pairings for a round.
     *
     * @param players List of players with current scores
     * @param previousPairings Set of already-played pairings (as sorted pairs of IDs)
     * @return List of pairings for this round
     */
    fun generatePairings(
        players: List<SwissPlayer>,
        previousPairings: Set<Pair<Long, Long>> = emptySet(),
    ): List<Pairing> {
        if (players.isEmpty()) return emptyList()
        if (players.size == 1) return listOf(Pairing(players[0].id, null))

        // Sort by score descending, then ELO descending
        val sorted = players.sortedWith(
            compareByDescending<SwissPlayer> { it.score }
                .thenByDescending { it.eloRating }
        )

        val pairings = mutableListOf<Pairing>()
        val paired = mutableSetOf<Long>()

        // Handle odd number: give bye to lowest-scoring unpaired player
        val byePlayer = if (sorted.size % 2 == 1) {
            // Find lowest-scoring player who hasn't had a bye yet
            sorted.lastOrNull { !previousPairings.contains(Pair(it.id, -1L)) }
                ?: sorted.last()
        } else null

        val toPair = if (byePlayer != null) {
            pairings.add(Pairing(byePlayer.id, null))
            paired.add(byePlayer.id)
            sorted.filter { it.id != byePlayer.id }
        } else {
            sorted
        }

        // Dutch system: group by score, then pair within groups
        val scoreGroups = toPair.groupBy { it.score }
            .toSortedMap(compareByDescending { it })

        val unpaired = mutableListOf<SwissPlayer>()

        for ((_, group) in scoreGroups) {
            val pool = (unpaired + group).toMutableList()
            unpaired.clear()

            while (pool.size >= 2) {
                val player = pool.removeFirst()
                if (paired.contains(player.id)) continue

                // Find best opponent: prefer someone from the same score group
                // who hasn't been paired with this player before
                val opponent = pool.firstOrNull { candidate ->
                    !paired.contains(candidate.id) &&
                        !hasPlayedBefore(player.id, candidate.id, previousPairings)
                } ?: pool.firstOrNull { !paired.contains(it.id) }

                if (opponent != null) {
                    pool.remove(opponent)
                    pairings.add(Pairing(player.id, opponent.id))
                    paired.add(player.id)
                    paired.add(opponent.id)
                } else {
                    unpaired.add(player)
                }
            }

            unpaired.addAll(pool.filter { !paired.contains(it.id) })
        }

        // Handle any remaining unpaired (shouldn't happen with even numbers, but safety)
        if (unpaired.size >= 2) {
            for (i in unpaired.indices step 2) {
                if (i + 1 < unpaired.size) {
                    pairings.add(Pairing(unpaired[i].id, unpaired[i + 1].id))
                }
            }
        }

        logger.info("Generated ${pairings.size} pairings (${pairings.count { it.player2Id == null }} byes)")
        return pairings
    }

    private fun hasPlayedBefore(p1: Long, p2: Long, previousPairings: Set<Pair<Long, Long>>): Boolean {
        val pair = if (p1 < p2) Pair(p1, p2) else Pair(p2, p1)
        return previousPairings.contains(pair)
    }

    /**
     * Create a normalized pairing key (smaller ID first) for tracking previous matchups.
     */
    fun pairingKey(p1: Long, p2: Long): Pair<Long, Long> {
        return if (p1 < p2) Pair(p1, p2) else Pair(p2, p1)
    }

    /**
     * Calculate tournament placement points based on final position and tournament size.
     */
    fun calculatePoints(placement: Int, totalPlayers: Int): Int {
        return when {
            placement == 1 -> 10
            placement == 2 -> 7
            placement <= 4 -> 5
            placement <= 8 -> 3
            else -> 1
        }
    }
}
