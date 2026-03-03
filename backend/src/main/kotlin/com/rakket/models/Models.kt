package com.rakket.models

import kotlinx.serialization.Serializable

@Serializable
data class PlayerDto(
    val id: Long,
    val slackUserId: String,
    val displayName: String,
    val avatarUrl: String? = null,
    val eloRating: Int = 1000,
    val totalMatches: Int = 0,
    val totalWins: Int = 0,
    val isAdmin: Boolean = false,
    val createdAt: String = "",
    val updatedAt: String = "",
)

@Serializable
data class TournamentDto(
    val id: Long,
    val date: String,
    val status: String,
    val participantCount: Int = 0,
    val totalRounds: Int = 0,
    val createdAt: String = "",
)

@Serializable
data class TournamentDetailDto(
    val tournament: TournamentDto,
    val participants: List<ParticipantDto> = emptyList(),
    val rounds: List<RoundDto> = emptyList(),
)

@Serializable
data class ParticipantDto(
    val playerId: Long,
    val displayName: String,
    val avatarUrl: String? = null,
    val finalPlacement: Int? = null,
    val pointsAwarded: Int = 0,
    val roundsWon: Int = 0,
)

@Serializable
data class RoundDto(
    val id: Long,
    val roundNumber: Int,
    val status: String,
    val matches: List<MatchDto> = emptyList(),
)

@Serializable
data class MatchDto(
    val id: Long,
    val player1: PlayerSummaryDto? = null,
    val player2: PlayerSummaryDto? = null,
    val winner: PlayerSummaryDto? = null,
    val status: String,
    val sets: List<SetScoreDto> = emptyList(),
    val completedAt: String? = null,
)

@Serializable
data class PlayerSummaryDto(
    val id: Long,
    val displayName: String,
    val avatarUrl: String? = null,
    val eloRating: Int = 1000,
)

@Serializable
data class SetScoreDto(
    val setNumber: Int,
    val player1Score: Int,
    val player2Score: Int,
)

@Serializable
data class ScoreReportRequest(
    val matchId: Long,
    val sets: List<SetScoreDto>,
)

@Serializable
data class EloHistoryDto(
    val id: Long,
    val playerId: Long,
    val matchId: Long? = null,
    val eloBefore: Int,
    val eloAfter: Int,
    val eloChange: Int,
    val recordedAt: String = "",
)

@Serializable
data class BadgeDto(
    val badgeType: String,
    val name: String,
    val description: String,
    val emoji: String,
    val earnedAt: String = "",
)

@Serializable
data class LeaderboardEntryDto(
    val rank: Int,
    val player: PlayerSummaryDto,
    val eloRating: Int,
    val totalMatches: Int,
    val totalWins: Int,
    val winRate: Double,
    val eloTrend: Int = 0,
)

@Serializable
data class HeadToHeadDto(
    val opponent: PlayerSummaryDto,
    val wins: Int,
    val losses: Int,
    val totalMatches: Int,
)

@Serializable
data class PlayerStatsDto(
    val player: PlayerDto,
    val tournamentsPlayed: Int = 0,
    val tournamentWins: Int = 0,
    val bestPlacement: Int? = null,
    val averagePlacement: Double? = null,
    val currentWinStreak: Int = 0,
    val longestWinStreak: Int = 0,
    val badges: List<BadgeDto> = emptyList(),
    val recentMatches: List<MatchDto> = emptyList(),
    val headToHead: List<HeadToHeadDto> = emptyList(),
)
