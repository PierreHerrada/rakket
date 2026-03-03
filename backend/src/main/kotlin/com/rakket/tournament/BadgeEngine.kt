package com.rakket.tournament

import com.rakket.db.*
import org.jetbrains.exposed.sql.*
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

private val logger = LoggerFactory.getLogger("com.rakket.tournament.BadgeEngine")

/**
 * Achievement engine that checks and awards badges after each match or tournament.
 */
object BadgeEngine {

    /**
     * Check and award badges after a match is completed.
     */
    fun checkMatchBadges(matchId: Long, winnerId: Long, loserId: Long, tournamentId: Long?) {
        val awarded = mutableListOf<Pair<Long, String>>()

        // FIRST_BLOOD: Win your first ever match
        if (isFirstWin(winnerId)) {
            if (awardBadge(winnerId, "FIRST_BLOOD", matchId = matchId)) {
                awarded.add(Pair(winnerId, "FIRST_BLOOD"))
            }
        }

        // ON_FIRE: Win 5 matches in a row
        if (getCurrentWinStreak(winnerId) >= 5) {
            if (awardBadge(winnerId, "ON_FIRE", matchId = matchId)) {
                awarded.add(Pair(winnerId, "ON_FIRE"))
            }
        }

        // THE_WALL: Win a set 11-0
        val sets = MatchSets.select { MatchSets.matchId eq matchId }.toList()
        for (set in sets) {
            val p1 = set[MatchSets.player1Score]
            val p2 = set[MatchSets.player2Score]
            if ((p1 == 11 && p2 == 0) || (p1 == 0 && p2 == 11)) {
                val shutoutWinner = if (p1 > p2) {
                    Matches.select { Matches.id eq matchId }.first()[Matches.player1Id]
                } else {
                    Matches.select { Matches.id eq matchId }.first()[Matches.player2Id]
                }
                if (shutoutWinner != null) {
                    if (awardBadge(shutoutWinner, "THE_WALL", matchId = matchId)) {
                        awarded.add(Pair(shutoutWinner, "THE_WALL"))
                    }
                }
            }
        }

        // GIANT_KILLER: Beat a player with 200+ higher ELO
        val winnerElo = Players.select { Players.id eq winnerId }.first()[Players.eloRating]
        val loserElo = Players.select { Players.id eq loserId }.first()[Players.eloRating]
        if (loserElo - winnerElo >= 200) {
            if (awardBadge(winnerId, "GIANT_KILLER", matchId = matchId)) {
                awarded.add(Pair(winnerId, "GIANT_KILLER"))
            }
        }

        // COMEBACK_KID: Win a match after losing the first set
        if (sets.isNotEmpty()) {
            val match = Matches.select { Matches.id eq matchId }.first()
            val firstSet = sets.minByOrNull { it[MatchSets.setNumber] }
            if (firstSet != null) {
                val p1Id = match[Matches.player1Id]
                val firstSetWinner = if (firstSet[MatchSets.player1Score] > firstSet[MatchSets.player2Score]) p1Id else match[Matches.player2Id]
                if (firstSetWinner != winnerId) {
                    if (awardBadge(winnerId, "COMEBACK_KID", matchId = matchId)) {
                        awarded.add(Pair(winnerId, "COMEBACK_KID"))
                    }
                }
            }
        }

        // RIVAL: Play the same opponent 10+ times
        val match = Matches.select { Matches.id eq matchId }.first()
        val p1 = match[Matches.player1Id]
        val p2 = match[Matches.player2Id]
        if (p1 != null && p2 != null) {
            val h2hCount = Matches.select {
                (((Matches.player1Id eq p1) and (Matches.player2Id eq p2)) or
                    ((Matches.player1Id eq p2) and (Matches.player2Id eq p1))) and
                    (Matches.status eq "completed")
            }.count()

            if (h2hCount >= 10) {
                if (awardBadge(p1, "RIVAL", matchId = matchId)) awarded.add(Pair(p1, "RIVAL"))
                if (awardBadge(p2, "RIVAL", matchId = matchId)) awarded.add(Pair(p2, "RIVAL"))
            }
        }

        // VETERAN: Play 100+ matches
        for (playerId in listOf(winnerId, loserId)) {
            val totalMatches = Players.select { Players.id eq playerId }.first()[Players.totalMatches]
            if (totalMatches >= 100) {
                if (awardBadge(playerId, "VETERAN", matchId = matchId)) {
                    awarded.add(Pair(playerId, "VETERAN"))
                }
            }
        }

        // SOCIAL_BUTTERFLY: Play against 20+ different opponents
        for (playerId in listOf(winnerId, loserId)) {
            val uniqueOpponents = getUniqueOpponentCount(playerId)
            if (uniqueOpponents >= 20) {
                if (awardBadge(playerId, "SOCIAL_BUTTERFLY", matchId = matchId)) {
                    awarded.add(Pair(playerId, "SOCIAL_BUTTERFLY"))
                }
            }
        }

        for ((playerId, badge) in awarded) {
            val playerName = Players.select { Players.id eq playerId }.first()[Players.displayName]
            logger.info("Badge awarded: $badge to $playerName (player #$playerId)")
        }
    }

    /**
     * Check and award badges after a tournament is completed.
     */
    fun checkTournamentBadges(tournamentId: Long) {
        val participants = TournamentParticipants
            .select { TournamentParticipants.tournamentId eq tournamentId }
            .toList()

        // DEBUTANT: Play your first tournament
        for (p in participants) {
            val playerId = p[TournamentParticipants.playerId]
            val totalTournaments = TournamentParticipants
                .select { TournamentParticipants.playerId eq playerId }
                .count()
            if (totalTournaments == 1L) {
                awardBadge(playerId, "DEBUTANT", tournamentId = tournamentId)
            }
        }

        // CHAMPION: Win a tournament
        val winner = participants.firstOrNull { it[TournamentParticipants.finalPlacement] == 1 }
        if (winner != null) {
            val winnerId = winner[TournamentParticipants.playerId]
            awardBadge(winnerId, "CHAMPION", tournamentId = tournamentId)

            // SNIPER: Win 10 tournaments
            val totalWins = TournamentParticipants
                .select {
                    (TournamentParticipants.playerId eq winnerId) and
                        (TournamentParticipants.finalPlacement eq 1)
                }
                .count()
            if (totalWins >= 10) {
                awardBadge(winnerId, "SNIPER", tournamentId = tournamentId)
            }

            // UNDISPUTED: Win 3 tournaments in a row
            if (hasConsecutiveWins(winnerId, 3)) {
                awardBadge(winnerId, "UNDISPUTED", tournamentId = tournamentId)
            }

            // PERFECT_GAME: Win without dropping a set
            if (hasPerfectTournament(winnerId, tournamentId)) {
                awardBadge(winnerId, "PERFECT_GAME", tournamentId = tournamentId)
            }
        }

        // CONSISTENT: Participate in 10 consecutive tournaments
        for (p in participants) {
            val playerId = p[TournamentParticipants.playerId]
            if (hasConsecutiveParticipation(playerId, 10)) {
                awardBadge(playerId, "CONSISTENT", tournamentId = tournamentId)
            }
        }

        // RISING_STAR: Gain 100+ ELO in a single week — checked via ELO history
        for (p in participants) {
            val playerId = p[TournamentParticipants.playerId]
            val weekAgo = LocalDateTime.now().minusDays(7)
            val recentChanges = EloHistory
                .select {
                    (EloHistory.playerId eq playerId) and
                        (EloHistory.recordedAt greaterEq weekAgo)
                }
                .sumOf { it[EloHistory.eloChange] }

            if (recentChanges >= 100) {
                awardBadge(playerId, "RISING_STAR", tournamentId = tournamentId)
            }
        }
    }

    private fun awardBadge(
        playerId: Long,
        badgeType: String,
        matchId: Long? = null,
        tournamentId: Long? = null,
    ): Boolean {
        val existing = Badges.select {
            (Badges.playerId eq playerId) and (Badges.badgeType eq badgeType)
        }.firstOrNull()

        if (existing != null) return false

        Badges.insert {
            it[Badges.playerId] = playerId
            it[Badges.badgeType] = badgeType
            it[Badges.matchId] = matchId
            it[Badges.tournamentId] = tournamentId
        }
        return true
    }

    private fun isFirstWin(playerId: Long): Boolean {
        return Players.select { Players.id eq playerId }.first()[Players.totalWins] == 1
    }

    private fun getCurrentWinStreak(playerId: Long): Int {
        val matches = Matches.select {
            ((Matches.player1Id eq playerId) or (Matches.player2Id eq playerId)) and
                (Matches.status eq "completed")
        }.orderBy(Matches.completedAt, SortOrder.DESC).toList()

        var streak = 0
        for (match in matches) {
            if (match[Matches.winnerId] == playerId) streak++ else break
        }
        return streak
    }

    private fun getUniqueOpponentCount(playerId: Long): Int {
        val opponents = mutableSetOf<Long>()
        Matches.select {
            ((Matches.player1Id eq playerId) or (Matches.player2Id eq playerId)) and
                (Matches.status eq "completed")
        }.forEach { match ->
            val opponent = if (match[Matches.player1Id] == playerId) {
                match[Matches.player2Id]
            } else {
                match[Matches.player1Id]
            }
            if (opponent != null) opponents.add(opponent)
        }
        return opponents.size
    }

    private fun hasConsecutiveWins(playerId: Long, count: Int): Boolean {
        val tournaments = TournamentParticipants
            .innerJoin(Tournaments, { tournamentId }, { Tournaments.id })
            .select { TournamentParticipants.playerId eq playerId }
            .orderBy(Tournaments.date, SortOrder.DESC)
            .toList()

        if (tournaments.size < count) return false

        return tournaments.take(count).all {
            it[TournamentParticipants.finalPlacement] == 1
        }
    }

    private fun hasPerfectTournament(playerId: Long, tournamentId: Long): Boolean {
        val rounds = TournamentRounds.select { TournamentRounds.tournamentId eq tournamentId }.toList()

        for (round in rounds) {
            val matches = Matches.select {
                (Matches.roundId eq round[TournamentRounds.id]) and
                    ((Matches.player1Id eq playerId) or (Matches.player2Id eq playerId)) and
                    (Matches.status eq "completed")
            }.toList()

            for (match in matches) {
                val sets = MatchSets.select { MatchSets.matchId eq match[Matches.id] }.toList()
                for (set in sets) {
                    val isPlayer1 = match[Matches.player1Id] == playerId
                    val playerScore = if (isPlayer1) set[MatchSets.player1Score] else set[MatchSets.player2Score]
                    val opponentScore = if (isPlayer1) set[MatchSets.player2Score] else set[MatchSets.player1Score]
                    if (opponentScore > playerScore) return false  // Lost a set
                }
            }
        }
        return true
    }

    private fun hasConsecutiveParticipation(playerId: Long, count: Int): Boolean {
        val tournaments = Tournaments.selectAll()
            .orderBy(Tournaments.date, SortOrder.DESC)
            .toList()

        if (tournaments.size < count) return false

        var consecutive = 0
        for (tournament in tournaments) {
            val participated = TournamentParticipants.select {
                (TournamentParticipants.tournamentId eq tournament[Tournaments.id]) and
                    (TournamentParticipants.playerId eq playerId)
            }.firstOrNull() != null

            if (participated) {
                consecutive++
                if (consecutive >= count) return true
            } else {
                consecutive = 0
            }
        }
        return false
    }
}
