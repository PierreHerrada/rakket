package com.rakket.tournament

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SwissPairingTest {

    @Test
    fun `calculateRounds returns correct number of rounds`() {
        assertEquals(0, SwissPairing.calculateRounds(1))
        assertEquals(1, SwissPairing.calculateRounds(2))
        assertEquals(2, SwissPairing.calculateRounds(3))
        assertEquals(2, SwissPairing.calculateRounds(4))
        assertEquals(3, SwissPairing.calculateRounds(5))
        assertEquals(3, SwissPairing.calculateRounds(8))
        assertEquals(4, SwissPairing.calculateRounds(9))
        assertEquals(4, SwissPairing.calculateRounds(16))
    }

    @Test
    fun `generatePairings returns empty for no players`() {
        val pairings = SwissPairing.generatePairings(emptyList())
        assertTrue(pairings.isEmpty())
    }

    @Test
    fun `generatePairings gives bye for single player`() {
        val players = listOf(SwissPlayer(1))
        val pairings = SwissPairing.generatePairings(players)
        assertEquals(1, pairings.size)
        assertEquals(1L, pairings[0].player1Id)
        assertEquals(null, pairings[0].player2Id)
    }

    @Test
    fun `generatePairings pairs two players correctly`() {
        val players = listOf(SwissPlayer(1, eloRating = 1200), SwissPlayer(2, eloRating = 1000))
        val pairings = SwissPairing.generatePairings(players)
        assertEquals(1, pairings.size)
        assertTrue(pairings[0].player2Id != null)
    }

    @Test
    fun `generatePairings handles odd number with bye`() {
        val players = listOf(
            SwissPlayer(1, score = 1.0, eloRating = 1200),
            SwissPlayer(2, score = 1.0, eloRating = 1100),
            SwissPlayer(3, score = 0.0, eloRating = 1000),
        )
        val pairings = SwissPairing.generatePairings(players)

        // Should have 2 pairings: one real match and one bye
        assertEquals(2, pairings.size)
        val byePairing = pairings.find { it.player2Id == null }
        assertTrue(byePairing != null, "Should have a bye pairing")
    }

    @Test
    fun `generatePairings avoids rematches`() {
        val players = listOf(
            SwissPlayer(1, score = 1.0, eloRating = 1200),
            SwissPlayer(2, score = 1.0, eloRating = 1100),
            SwissPlayer(3, score = 0.0, eloRating = 1050),
            SwissPlayer(4, score = 0.0, eloRating = 1000),
        )

        // Players 1-2 and 3-4 already played
        val previousPairings = setOf(
            SwissPairing.pairingKey(1, 2),
            SwissPairing.pairingKey(3, 4),
        )

        val pairings = SwissPairing.generatePairings(players, previousPairings)
        assertEquals(2, pairings.size)

        // Verify no rematches
        for (p in pairings) {
            if (p.player2Id != null) {
                val key = SwissPairing.pairingKey(p.player1Id, p.player2Id!!)
                assertTrue(
                    !previousPairings.contains(key),
                    "Pairing ${p.player1Id} vs ${p.player2Id} is a rematch"
                )
            }
        }
    }

    @Test
    fun `generatePairings handles 8 players`() {
        val players = (1..8).map { SwissPlayer(it.toLong(), eloRating = 1000 + it * 50) }
        val pairings = SwissPairing.generatePairings(players)

        assertEquals(4, pairings.size)
        assertTrue(pairings.all { it.player2Id != null }, "No byes with even players")

        // Verify all players are paired exactly once
        val allPlayerIds = pairings.flatMap { listOfNotNull(it.player1Id, it.player2Id) }
        assertEquals(8, allPlayerIds.distinct().size)
    }

    @Test
    fun `calculatePoints returns correct values`() {
        assertEquals(10, SwissPairing.calculatePoints(1, 8))
        assertEquals(7, SwissPairing.calculatePoints(2, 8))
        assertEquals(5, SwissPairing.calculatePoints(3, 8))
        assertEquals(5, SwissPairing.calculatePoints(4, 8))
        assertEquals(3, SwissPairing.calculatePoints(5, 8))
        assertEquals(3, SwissPairing.calculatePoints(8, 8))
        assertEquals(1, SwissPairing.calculatePoints(9, 16))
    }

    @Test
    fun `pairingKey is normalized`() {
        assertEquals(SwissPairing.pairingKey(1, 2), SwissPairing.pairingKey(2, 1))
        assertEquals(Pair(1L, 2L), SwissPairing.pairingKey(1, 2))
        assertEquals(Pair(1L, 2L), SwissPairing.pairingKey(2, 1))
    }
}
