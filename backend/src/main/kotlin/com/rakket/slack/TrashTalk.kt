package com.rakket.slack

import kotlin.random.Random

/**
 * Fun trash talk comments posted after match results.
 */
object TrashTalk {

    private val comments = listOf(
        ":fire: {winner} crushed {loser} 2-0. Someone call the fire department!",
        ":sunglasses: {winner} made it look easy against {loser}.",
        ":wave: {loser}, better luck next time!",
        ":muscle: {winner} flexing on {loser} today.",
        ":eyes: {loser} might want to hit the practice table after that one.",
        ":rocket: {winner} is on a mission! {loser} never stood a chance.",
        ":chart_with_upwards_trend: {winner} climbing the ranks one match at a time.",
        ":tornado: {winner} blew through {loser} like a tornado!",
        ":ice_cream: That was cold, {winner}. Ice cold.",
        ":ping_pong: Another day, another W for {winner}.",
    )

    /**
     * Get a random trash talk comment. Returns null 60% of the time to avoid spamming.
     */
    fun getComment(winnerName: String, loserName: String): String? {
        if (Random.nextFloat() > 0.4f) return null

        return comments.random()
            .replace("{winner}", winnerName)
            .replace("{loser}", loserName)
    }
}
