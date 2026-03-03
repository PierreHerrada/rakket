package com.rakket.tournament

import com.rakket.models.SetScoreDto
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ScoreValidatorTest {
    @Test
    fun `validateSet accepts normal 11-point wins`() {
        assertTrue(ScoreValidator.validateSet(11, 0).valid)
        assertTrue(ScoreValidator.validateSet(11, 5).valid)
        assertTrue(ScoreValidator.validateSet(11, 9).valid)
        assertTrue(ScoreValidator.validateSet(0, 11).valid)
    }

    @Test
    fun `validateSet accepts deuce wins`() {
        assertTrue(ScoreValidator.validateSet(12, 10).valid)
        assertTrue(ScoreValidator.validateSet(15, 13).valid)
        assertTrue(ScoreValidator.validateSet(10, 12).valid)
    }

    @Test
    fun `validateSet rejects ties`() {
        assertFalse(ScoreValidator.validateSet(11, 11).valid)
        assertFalse(ScoreValidator.validateSet(0, 0).valid)
    }

    @Test
    fun `validateSet rejects invalid scores`() {
        assertFalse(ScoreValidator.validateSet(11, 10).valid) // Not win by 2
        assertFalse(ScoreValidator.validateSet(13, 10).valid) // Should be 12-10
        assertFalse(ScoreValidator.validateSet(10, 5).valid) // Not to 11
        assertFalse(ScoreValidator.validateSet(-1, 11).valid) // Negative
    }

    @Test
    fun `validateBestOf3 accepts 2-0 wins`() {
        val sets =
            listOf(
                SetScoreDto(1, 11, 5),
                SetScoreDto(2, 11, 7),
            )
        val result = ScoreValidator.validateBestOf3(sets, 1, 2)
        assertTrue(result.valid)
        assertEquals(1L, result.winnerId)
    }

    @Test
    fun `validateBestOf3 accepts 2-1 wins`() {
        val sets =
            listOf(
                SetScoreDto(1, 11, 7),
                SetScoreDto(2, 9, 11),
                SetScoreDto(3, 11, 5),
            )
        val result = ScoreValidator.validateBestOf3(sets, 1, 2)
        assertTrue(result.valid)
        assertEquals(1L, result.winnerId)
    }

    @Test
    fun `validateBestOf3 identifies player2 as winner`() {
        val sets =
            listOf(
                SetScoreDto(1, 5, 11),
                SetScoreDto(2, 7, 11),
            )
        val result = ScoreValidator.validateBestOf3(sets, 1, 2)
        assertTrue(result.valid)
        assertEquals(2L, result.winnerId)
    }

    @Test
    fun `validateBestOf3 rejects empty sets`() {
        val result = ScoreValidator.validateBestOf3(emptyList(), 1, 2)
        assertFalse(result.valid)
    }

    @Test
    fun `validateBestOf3 rejects 4 sets`() {
        val sets =
            listOf(
                SetScoreDto(1, 11, 5),
                SetScoreDto(2, 5, 11),
                SetScoreDto(3, 11, 7),
                SetScoreDto(4, 11, 3),
            )
        val result = ScoreValidator.validateBestOf3(sets, 1, 2)
        assertFalse(result.valid)
    }

    @Test
    fun `validateBestOf3 rejects extra set after 2-0`() {
        val sets =
            listOf(
                SetScoreDto(1, 11, 5),
                SetScoreDto(2, 11, 7),
                // Unnecessary third set
                SetScoreDto(3, 11, 3),
            )
        val result = ScoreValidator.validateBestOf3(sets, 1, 2)
        assertFalse(result.valid)
    }

    @Test
    fun `validateBestOf3 rejects incomplete match`() {
        val sets =
            listOf(
                SetScoreDto(1, 11, 5),
                SetScoreDto(2, 5, 11),
            )
        val result = ScoreValidator.validateBestOf3(sets, 1, 2)
        assertFalse(result.valid) // 1-1, no winner yet
    }
}
