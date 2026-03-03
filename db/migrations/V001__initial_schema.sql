-- Players
CREATE TABLE players (
    id BIGSERIAL PRIMARY KEY,
    slack_user_id VARCHAR(32) UNIQUE NOT NULL,
    display_name VARCHAR(255) NOT NULL,
    avatar_url TEXT,
    elo_rating INTEGER NOT NULL DEFAULT 1000,
    total_matches INTEGER NOT NULL DEFAULT 0,
    total_wins INTEGER NOT NULL DEFAULT 0,
    is_admin BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_players_slack_user_id ON players(slack_user_id);
CREATE INDEX idx_players_elo_rating ON players(elo_rating DESC);

-- Tournaments
CREATE TABLE tournaments (
    id BIGSERIAL PRIMARY KEY,
    date DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'registration',
    slack_message_ts VARCHAR(64),
    participant_count INTEGER NOT NULL DEFAULT 0,
    total_rounds INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_tournaments_date ON tournaments(date DESC);
CREATE INDEX idx_tournaments_status ON tournaments(status);

-- Tournament participants
CREATE TABLE tournament_participants (
    id BIGSERIAL PRIMARY KEY,
    tournament_id BIGINT NOT NULL REFERENCES tournaments(id) ON DELETE CASCADE,
    player_id BIGINT NOT NULL REFERENCES players(id) ON DELETE CASCADE,
    final_placement INTEGER,
    points_awarded INTEGER NOT NULL DEFAULT 0,
    rounds_won INTEGER NOT NULL DEFAULT 0,
    UNIQUE(tournament_id, player_id)
);

CREATE INDEX idx_tournament_participants_tournament ON tournament_participants(tournament_id);
CREATE INDEX idx_tournament_participants_player ON tournament_participants(player_id);

-- Tournament rounds
CREATE TABLE tournament_rounds (
    id BIGSERIAL PRIMARY KEY,
    tournament_id BIGINT NOT NULL REFERENCES tournaments(id) ON DELETE CASCADE,
    round_number INTEGER NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'pending',
    UNIQUE(tournament_id, round_number)
);

CREATE INDEX idx_tournament_rounds_tournament ON tournament_rounds(tournament_id);

-- Matches
CREATE TABLE matches (
    id BIGSERIAL PRIMARY KEY,
    round_id BIGINT NOT NULL REFERENCES tournament_rounds(id) ON DELETE CASCADE,
    player1_id BIGINT REFERENCES players(id),
    player2_id BIGINT REFERENCES players(id),
    winner_id BIGINT REFERENCES players(id),
    status VARCHAR(20) NOT NULL DEFAULT 'pending',
    reported_by BIGINT REFERENCES players(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_matches_round ON matches(round_id);
CREATE INDEX idx_matches_player1 ON matches(player1_id);
CREATE INDEX idx_matches_player2 ON matches(player2_id);
CREATE INDEX idx_matches_status ON matches(status);

-- Match sets (individual set scores within a match)
CREATE TABLE match_sets (
    id BIGSERIAL PRIMARY KEY,
    match_id BIGINT NOT NULL REFERENCES matches(id) ON DELETE CASCADE,
    set_number INTEGER NOT NULL,
    player1_score INTEGER NOT NULL,
    player2_score INTEGER NOT NULL,
    UNIQUE(match_id, set_number)
);

CREATE INDEX idx_match_sets_match ON match_sets(match_id);

-- ELO history
CREATE TABLE elo_history (
    id BIGSERIAL PRIMARY KEY,
    player_id BIGINT NOT NULL REFERENCES players(id) ON DELETE CASCADE,
    match_id BIGINT REFERENCES matches(id) ON DELETE SET NULL,
    elo_before INTEGER NOT NULL,
    elo_after INTEGER NOT NULL,
    elo_change INTEGER NOT NULL,
    recorded_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_elo_history_player ON elo_history(player_id);
CREATE INDEX idx_elo_history_player_time ON elo_history(player_id, recorded_at DESC);

-- Badges
CREATE TABLE badges (
    id BIGSERIAL PRIMARY KEY,
    player_id BIGINT NOT NULL REFERENCES players(id) ON DELETE CASCADE,
    badge_type VARCHAR(50) NOT NULL,
    earned_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    match_id BIGINT REFERENCES matches(id) ON DELETE SET NULL,
    tournament_id BIGINT REFERENCES tournaments(id) ON DELETE SET NULL,
    UNIQUE(player_id, badge_type)
);

CREATE INDEX idx_badges_player ON badges(player_id);
CREATE INDEX idx_badges_type ON badges(badge_type);
