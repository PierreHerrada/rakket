package com.rakket.db

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.javatime.date
import java.time.LocalDateTime

object Players : Table("players") {
    val id = long("id").autoIncrement()
    val slackUserId = varchar("slack_user_id", 32).uniqueIndex()
    val displayName = varchar("display_name", 255)
    val avatarUrl = text("avatar_url").nullable()
    val eloRating = integer("elo_rating").default(1000)
    val totalMatches = integer("total_matches").default(0)
    val totalWins = integer("total_wins").default(0)
    val isAdmin = bool("is_admin").default(false)
    val createdAt = datetime("created_at").defaultExpression(org.jetbrains.exposed.sql.javatime.CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(org.jetbrains.exposed.sql.javatime.CurrentDateTime)

    override val primaryKey = PrimaryKey(id)
}

object Tournaments : Table("tournaments") {
    val id = long("id").autoIncrement()
    val date = date("date")
    val status = varchar("status", 20).default("registration")
    val slackMessageTs = varchar("slack_message_ts", 64).nullable()
    val participantCount = integer("participant_count").default(0)
    val totalRounds = integer("total_rounds").default(0)
    val createdAt = datetime("created_at").defaultExpression(org.jetbrains.exposed.sql.javatime.CurrentDateTime)

    override val primaryKey = PrimaryKey(id)
}

object TournamentParticipants : Table("tournament_participants") {
    val id = long("id").autoIncrement()
    val tournamentId = long("tournament_id").references(Tournaments.id)
    val playerId = long("player_id").references(Players.id)
    val finalPlacement = integer("final_placement").nullable()
    val pointsAwarded = integer("points_awarded").default(0)
    val roundsWon = integer("rounds_won").default(0)

    override val primaryKey = PrimaryKey(id)

    init {
        uniqueIndex(tournamentId, playerId)
    }
}

object TournamentRounds : Table("tournament_rounds") {
    val id = long("id").autoIncrement()
    val tournamentId = long("tournament_id").references(Tournaments.id)
    val roundNumber = integer("round_number")
    val status = varchar("status", 20).default("pending")

    override val primaryKey = PrimaryKey(id)

    init {
        uniqueIndex(tournamentId, roundNumber)
    }
}

object Matches : Table("matches") {
    val id = long("id").autoIncrement()
    val roundId = long("round_id").references(TournamentRounds.id)
    val player1Id = long("player1_id").references(Players.id).nullable()
    val player2Id = long("player2_id").references(Players.id).nullable()
    val winnerId = long("winner_id").references(Players.id).nullable()
    val status = varchar("status", 20).default("pending")
    val reportedBy = long("reported_by").references(Players.id).nullable()
    val createdAt = datetime("created_at").defaultExpression(org.jetbrains.exposed.sql.javatime.CurrentDateTime)
    val completedAt = datetime("completed_at").nullable()

    override val primaryKey = PrimaryKey(id)
}

object MatchSets : Table("match_sets") {
    val id = long("id").autoIncrement()
    val matchId = long("match_id").references(Matches.id)
    val setNumber = integer("set_number")
    val player1Score = integer("player1_score")
    val player2Score = integer("player2_score")

    override val primaryKey = PrimaryKey(id)

    init {
        uniqueIndex(matchId, setNumber)
    }
}

object EloHistory : Table("elo_history") {
    val id = long("id").autoIncrement()
    val playerId = long("player_id").references(Players.id)
    val matchId = long("match_id").references(Matches.id).nullable()
    val eloBefore = integer("elo_before")
    val eloAfter = integer("elo_after")
    val eloChange = integer("elo_change")
    val recordedAt = datetime("recorded_at").defaultExpression(org.jetbrains.exposed.sql.javatime.CurrentDateTime)

    override val primaryKey = PrimaryKey(id)
}

object Badges : Table("badges") {
    val id = long("id").autoIncrement()
    val playerId = long("player_id").references(Players.id)
    val badgeType = varchar("badge_type", 50)
    val earnedAt = datetime("earned_at").defaultExpression(org.jetbrains.exposed.sql.javatime.CurrentDateTime)
    val matchId = long("match_id").references(Matches.id).nullable()
    val tournamentId = long("tournament_id").references(Tournaments.id).nullable()

    override val primaryKey = PrimaryKey(id)

    init {
        uniqueIndex(playerId, badgeType)
    }
}
