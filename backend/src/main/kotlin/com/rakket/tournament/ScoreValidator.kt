package com.rakket.tournament

import com.rakket.models.SetScoreDto

/**
 * Validates match scores according to table tennis rules.
 */
object ScoreValidator {

    data class ValidationResult(
        val valid: Boolean,
        val error: String? = null,
        val winnerId: Long? = null,
    )

    /**
     * Validate a best-of-3 match score.
     * Each set is played to 11 points, win by 2.
     * A player must win 2 sets to win the match.
     */
    fun validateBestOf3(
        sets: List<SetScoreDto>,
        player1Id: Long,
        player2Id: Long,
    ): ValidationResult {
        if (sets.isEmpty() || sets.size > 3) {
            return ValidationResult(false, "Best of 3 requires 2 or 3 sets")
        }

        var p1Sets = 0
        var p2Sets = 0

        for ((index, set) in sets.withIndex()) {
            val validation = validateSet(set.player1Score, set.player2Score)
            if (!validation.valid) {
                return ValidationResult(false, "Set ${index + 1}: ${validation.error}")
            }

            if (set.player1Score > set.player2Score) p1Sets++ else p2Sets++

            // Match is over once someone has 2 set wins
            if (p1Sets == 2 || p2Sets == 2) {
                if (index + 1 != sets.size) {
                    return ValidationResult(false, "Match ended at set ${index + 1} but ${sets.size} sets reported")
                }
                break
            }
        }

        if (p1Sets < 2 && p2Sets < 2) {
            return ValidationResult(false, "No player has won 2 sets yet")
        }

        val winnerId = if (p1Sets == 2) player1Id else player2Id
        return ValidationResult(true, winnerId = winnerId)
    }

    /**
     * Validate a single set to 11 (must win by 2).
     */
    fun validateSet(score1: Int, score2: Int): ValidationResult {
        if (score1 < 0 || score2 < 0) {
            return ValidationResult(false, "Scores cannot be negative")
        }

        if (score1 == score2) {
            return ValidationResult(false, "Set cannot end in a tie")
        }

        val winner = maxOf(score1, score2)
        val loser = minOf(score1, score2)

        // Normal win: 11-X where X < 10
        if (winner == 11 && loser < 10) {
            return ValidationResult(true)
        }

        // Deuce win: both >= 10, winner leads by exactly 2
        if (winner >= 11 && loser >= 10 && winner - loser == 2) {
            return ValidationResult(true)
        }

        return ValidationResult(false, "Invalid set score: $score1-$score2 (must be to 11, win by 2)")
    }

    /**
     * Validate a single-set match to the given target score.
     */
    fun validateSingleSet(
        score1: Int,
        score2: Int,
        player1Id: Long,
        player2Id: Long,
        target: Int = 11,
    ): ValidationResult {
        val setValidation = validateSet(score1, score2)
        if (!setValidation.valid) return setValidation

        val winnerId = if (score1 > score2) player1Id else player2Id
        return ValidationResult(true, winnerId = winnerId)
    }
}
