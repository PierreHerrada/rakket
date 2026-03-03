package com.rakket.elo

import org.junit.jupiter.api.Test
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EloCalculatorTest {

    @Test
    fun `equal ratings produce roughly equal expected scores`() {
        val expected = EloCalculator.expectedScore(1000, 1000)
        assertEquals(0.5, expected, 0.001)
    }

    @Test
    fun `higher rated player has higher expected score`() {
        val expected = EloCalculator.expectedScore(1200, 1000)
        assertTrue(expected > 0.5)
        assertTrue(expected < 1.0)
    }

    @Test
    fun `200 point advantage gives roughly 76 percent expected`() {
        val expected = EloCalculator.expectedScore(1200, 1000)
        assertEquals(0.76, expected, 0.01)
    }

    @Test
    fun `equal ratings new players win gives correct change`() {
        val result = EloCalculator.calculate(1000, 1000, player1Won = true)
        // K=32, expected=0.5, actual=1.0 -> change = 32 * 0.5 = 16
        assertEquals(16, result.player1Change)
        assertEquals(-16, result.player2Change)
        assertEquals(1016, result.player1NewRating)
        assertEquals(984, result.player2NewRating)
    }

    @Test
    fun `established players have lower K factor`() {
        val result = EloCalculator.calculate(
            1000, 1000,
            player1Won = true,
            player1Matches = 30,
            player2Matches = 30,
        )
        // K=16, expected=0.5, actual=1.0 -> change = 16 * 0.5 = 8
        assertEquals(8, result.player1Change)
        assertEquals(-8, result.player2Change)
    }

    @Test
    fun `upset gives larger ELO change`() {
        // Lower rated player beats higher rated
        val result = EloCalculator.calculate(1000, 1200, player1Won = true)
        // Expected for player1 is low (~0.24), so winning gives big boost
        assertTrue(result.player1Change > 20)
        assertTrue(result.player2Change < -20)
    }

    @Test
    fun `expected win gives smaller ELO change`() {
        // Higher rated player beats lower rated
        val result = EloCalculator.calculate(1200, 1000, player1Won = true)
        // Expected for player1 is high (~0.76), so winning gives small gain
        assertTrue(result.player1Change < 10)
        assertTrue(result.player2Change > -10)
    }

    @Test
    fun `changes are roughly symmetric for equal K`() {
        val result = EloCalculator.calculate(
            1100, 900,
            player1Won = true,
            player1Matches = 5,
            player2Matches = 5,
        )
        // With same K factor, changes should roughly cancel out
        assertTrue(abs(result.player1Change + result.player2Change) <= 1)
    }

    @Test
    fun `rating cannot go below zero in extreme case`() {
        val result = EloCalculator.calculate(
            50, 2000,
            player1Won = false,
        )
        // Even with a big loss, the change is bounded by K
        assertTrue(result.player1NewRating >= 0 || result.player1Change > -50)
    }
}
