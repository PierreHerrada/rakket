# Project Prompt: Rakket 🏓

> **Rakket** — A self-hosted, Slack-integrated ping pong tournament manager with ELO rankings, Swiss-system brackets, achievements, and a beautiful web dashboard. Built with Kotlin/Ktor + React + PostgreSQL. Ships as a single Docker image.

---

## Overview

Build an open-source ping pong tournament management application called **Rakket**. It is a self-hosted, Dockerized app with a Slack bot integration and a React web UI, designed for office/team ping pong tournaments. It should be generic enough for anyone to deploy via environment variables.

**Repository name:** `rakket`
**Tagline:** *"Your office ping pong league, automated."*
**License:** MIT

---

## Tech Stack

| Layer | Technology |
|---|---|
| **Backend** | Kotlin + Ktor (lightweight, coroutine-based) |
| **Frontend** | React (TypeScript) with Tailwind CSS |
| **Database** | PostgreSQL (external, connection via env vars) |
| **DB Migrations** | Simple SQL-file-based migration system (numbered files in `db/migrations/`, applied in order at startup, tracked in a `schema_version` table — no heavy framework) |
| **Slack Integration** | Slack Bolt SDK for JVM (or Slack Web API via Ktor HTTP client) |
| **Auth** | Slack OAuth 2.0 (only workspace members can access the web UI) |
| **Packaging** | Single multi-stage Dockerfile producing a lightweight image (build frontend → build backend → copy into a slim JVM runtime like Eclipse Temurin Alpine) |
| **Orchestration** | docker-compose.yml included for local development (app + postgres) |
| **Documentation site** | GitHub Pages (static site in `/docs` folder, auto-deployed) |

---

## Architecture

### Monorepo Structure

```
rakket/
├── .github/
│   ├── ISSUE_TEMPLATE/
│   │   ├── bug_report.md          # Bug report template
│   │   ├── feature_request.md     # Feature request template
│   │   └── config.yml             # Issue template chooser config
│   ├── PULL_REQUEST_TEMPLATE.md   # PR template
│   ├── workflows/
│   │   ├── ci.yml                 # Build + test on PR
│   │   ├── release.yml            # Build & push Docker image on tag
│   │   └── docs.yml              # Deploy GitHub Pages on push to main
│   ├── FUNDING.yml                # Sponsorship links (optional)
│   └── CODEOWNERS                 # Code review assignments
├── backend/                       # Kotlin/Ktor project
│   ├── src/main/kotlin/
│   │   ├── Application.kt        # Ktor entry point
│   │   ├── config/               # Environment config, DB config
│   │   ├── db/                   # Database access (use Exposed or plain JDBC)
│   │   ├── migrations/           # Migration runner
│   │   ├── routes/               # REST API routes
│   │   ├── slack/                # Slack bot logic (commands, events, scheduled messages)
│   │   ├── tournament/           # Tournament engine (Swiss system, bracket generation)
│   │   ├── elo/                  # ELO rating calculator
│   │   └── models/               # Data classes
│   ├── src/test/kotlin/           # Unit & integration tests
│   └── build.gradle.kts
├── frontend/                      # React TypeScript project
│   ├── src/
│   │   ├── pages/                # Dashboard, Tournament, Leaderboard, Profile, History
│   │   └── components/           # Shared UI components
│   ├── package.json
│   └── vite.config.ts
├── db/
│   └── migrations/               # Numbered SQL files: V001__initial_schema.sql, etc.
├── docs/                         # GitHub Pages site (project documentation)
│   ├── index.html                # Landing page with hero, features, screenshots
│   ├── getting-started.md        # Quick start guide
│   ├── configuration.md          # Env var reference
│   ├── slack-setup.md            # Slack app creation walkthrough
│   ├── architecture.md           # Architecture overview + diagrams
│   ├── api-reference.md          # REST API documentation
│   └── assets/                   # Screenshots, logo, diagrams
├── Dockerfile                    # Multi-stage build
├── docker-compose.yml            # App + Postgres for local dev
├── .env.example                  # All configurable env vars
├── README.md                     # Project README (see section below)
├── CONTRIBUTING.md               # How to contribute
├── CODE_OF_CONDUCT.md            # Contributor Covenant
├── CHANGELOG.md                  # Keep a changelog format
├── SECURITY.md                   # Security policy & vulnerability reporting
└── LICENSE                       # MIT License
```

### Environment Variables (.env.example)

```env
# Database
DATABASE_URL=jdbc:postgresql://postgres:5432/rakket
DATABASE_USER=rakket
DATABASE_PASSWORD=changeme

# Slack
SLACK_BOT_TOKEN=xoxb-...
SLACK_SIGNING_SECRET=...
SLACK_CLIENT_ID=...
SLACK_CLIENT_SECRET=...
SLACK_CHANNEL_ID=C...          # Channel where the bot posts

# App
APP_URL=http://localhost:8080  # Public URL for OAuth callbacks
TIMEZONE=Europe/Paris
TOURNAMENT_DAY=MONDAY          # Day of the weekly tournament
REGISTRATION_TIME=09:00        # When the bot asks who wants to play
TOURNAMENT_TIME=16:00          # When brackets are generated
MATCH_FORMAT=BEST_OF_3         # BEST_OF_3, SINGLE_SET_11, SINGLE_SET_21
```

---

## Open Source Project Essentials

### README.md

The README must include:
- **Logo/banner** at the top (design a simple SVG logo — a ping pong paddle with a lightning bolt or bracket motif).
- **Badges**: CI status, Docker image size, license, latest release, GitHub stars.
- **One-line description** + tagline.
- **Screenshot/GIF** of the dashboard (placeholder initially, replace once built).
- **Features list** — concise bullet points with emojis.
- **Quick Start** section — `docker-compose up` in 3 commands.
- **Slack Setup** — brief steps + link to full docs.
- **Tech Stack** — small table.
- **Configuration** — link to full env var reference.
- **Contributing** — link to CONTRIBUTING.md.
- **License** — MIT.
- **Star History** badge + link.

### CONTRIBUTING.md

Include:
- How to set up the development environment.
- Code style guidelines (ktlint for Kotlin, ESLint + Prettier for React).
- Branch naming conventions (`feature/`, `fix/`, `docs/`).
- Commit message format (Conventional Commits: `feat:`, `fix:`, `docs:`, `chore:`).
- PR process: describe the change, link issue, add screenshots for UI changes.
- How to run tests.
- Issue & PR labels explained.

### CODE_OF_CONDUCT.md

Use the Contributor Covenant v2.1 (standard for open-source projects).

### SECURITY.md

- How to report vulnerabilities (email, not public issues).
- Supported versions table.
- Response timeline commitment.

### CHANGELOG.md

- Follow "Keep a Changelog" format (keepachangelog.com).
- Sections: Added, Changed, Deprecated, Removed, Fixed, Security.
- Start with `## [Unreleased]`.

### GitHub Issue Templates

**Bug Report (`bug_report.md`):**
- Description, steps to reproduce, expected behavior, actual behavior.
- Environment: Docker version, OS, browser.
- Screenshots section.
- Severity label selector.

**Feature Request (`feature_request.md`):**
- Problem description, proposed solution, alternatives considered.
- Additional context.

### PR Template (`PULL_REQUEST_TEMPLATE.md`)

- Description of changes.
- Type: bug fix / feature / docs / refactor.
- Checklist: tests added, docs updated, changelog updated, screenshots for UI.
- Related issue(s).

### GitHub Actions Workflows

**CI (`ci.yml`):**
- Trigger: push to main, PRs.
- Jobs: lint Kotlin (ktlint), lint frontend (ESLint), build backend, build frontend, run tests, build Docker image (no push).

**Release (`release.yml`):**
- Trigger: tag `v*`.
- Build multi-platform Docker image (amd64 + arm64).
- Push to GitHub Container Registry (ghcr.io).
- Create GitHub Release with changelog excerpt.
- Attach release notes.

**Docs (`docs.yml`):**
- Trigger: push to `main` that modifies `docs/` folder.
- Deploy to GitHub Pages.

### GitHub Pages Documentation Site

Build a clean, static documentation site in the `/docs` folder:
- **Landing page** (`index.html`): Hero section with logo, tagline, "Get Started" button, feature cards, screenshot carousel, footer with links.
- Use a simple static site generator approach (plain HTML + CSS, or a lightweight tool like Docsify or MkDocs — keep it simple).
- Pages: Getting Started, Configuration, Slack Setup Guide (with screenshots of Slack API dashboard), Architecture Overview, API Reference, FAQ.
- Mobile-responsive.
- Dark mode support matching the main app.

### GitHub Repository Settings (document in README)

Recommend enabling:
- **Discussions** — for community Q&A.
- **Projects** — for roadmap tracking.
- **Wiki** — redirect to docs site.
- **Topics/Tags**: `ping-pong`, `table-tennis`, `tournament`, `slack-bot`, `kotlin`, `ktor`, `react`, `docker`, `elo-rating`, `swiss-system`, `open-source`.

---

## Core Features

### 1. Weekly Tournament Lifecycle

**Monday morning (configurable time):**
- The Slack bot posts a message to the configured channel: *"🏓 It's tournament day! React with 🏓 to join today's tournament. Registration closes at 16:00."*
- The message includes a countdown or a reminder of the cut-off time.

**Monday at 16:00 (configurable):**
- Bot closes registration by reading reactions on the message.
- Based on the number of participants, the **Swiss-system tournament** is generated:
  - Calculate the optimal number of rounds: `ceil(log2(n))` rounds.
  - Round 1: random pairing.
  - Subsequent rounds: pair players with similar scores (standard Swiss pairing rules — no repeat matchups, balance colors/sides).
- Bot posts the Round 1 matchups to Slack with a visual bracket or table.
- Bot also sends DMs to each participant with their first opponent.

**During the tournament:**
- After each match, either player (or both) can report the score via:
  - **Slack**: slash command `/rakket score @opponent 2-1` (or interactive modal triggered by a button on the match message).
  - **Web UI**: click on the match and enter the set scores (e.g., 11-7, 9-11, 11-5).
- The system validates: best of 3 sets, each set to 11 (win by 2).
- When a round is complete (all matches reported), the bot:
  - Posts results for the round.
  - Generates next-round pairings (Swiss pairing algorithm).
  - Posts next-round matchups.
- After the final round, the bot announces the winner and podium.

**Edge cases to handle:**
- Odd number of players → one player gets a bye (automatic win) per round.
- Player forfeits / no-shows.
- Score disputes (flag for admin resolution).
- Minimum 3 players to run a tournament; if fewer, bot announces cancellation.

### 2. Score Reporting

**Via Slack:**
- Slash command: `/rakket score @opponent 11-7 9-11 11-5`
- Or: interactive button on match messages → opens a modal with set score inputs.
- Bot confirms the score in the channel thread.
- If both players report conflicting scores, flag as disputed.

**Via Web UI:**
- Navigate to current tournament → click active match → enter set scores.
- Real-time update via WebSocket or polling.

### 3. Leaderboard & ELO System

**ELO Rating:**
- Every player starts at 1000 ELO.
- After each match, update ELO using standard formula: `K=32` for new players (< 20 matches), `K=16` for established players.
- ELO changes are shown after each match in Slack and the web UI.

**Tournament Points:**
- Weekly tournament placement awards points:
  - 1st place: 10 pts, 2nd: 7 pts, 3rd-4th: 5 pts, 5th-8th: 3 pts, participation: 1 pt.
  - Scale dynamically based on tournament size.

**Leaderboard Page (Web UI):**
- Default view: all-time ELO ranking.
- Toggle: tournament points (cumulative).
- Filters: last 4 weeks, last 12 weeks, all-time.
- Show rank, player name, Slack avatar, ELO, ELO trend (↑↓), total matches, win rate.
- Highlight the current week's champion.

### 4. Tournament History

**Past Results Page:**
- List of all past tournaments with date, number of participants, winner.
- Click into any tournament to see: full bracket/Swiss rounds, all match scores, ELO changes.
- Search/filter by date range.

### 5. Player Profiles

**Profile Page (per player):**
- Slack avatar, display name, join date.
- Current ELO + ELO history chart (line graph over time).
- Stats: total matches, wins, losses, win rate, tournaments played, tournament wins, best placement, average placement.
- Win streak (current & longest).
- Recent match history.
- Head-to-head records: table showing record against every opponent they've faced.
- Achievements/badges (see below).

### 6. Achievements & Badges

Award badges automatically when conditions are met. Display on profile. Announce in Slack when earned. Examples:

- 🏆 **Champion** — Win a tournament.
- 🔥 **On Fire** — Win 5 matches in a row.
- 🧱 **The Wall** — Win a set 11-0.
- 🎯 **Sniper** — Win 10 tournaments.
- 🐣 **First Blood** — Win your first ever match.
- 💪 **Giant Killer** — Beat a player with 200+ higher ELO.
- 📈 **Rising Star** — Gain 100+ ELO in a single week.
- 🎳 **Perfect Game** — Win a tournament without dropping a single set.
- 🤝 **Rival** — Play the same opponent 10+ times.
- 🏅 **Consistent** — Participate in 10 consecutive tournaments.
- 🔄 **Comeback Kid** — Win a match after losing the first set.
- 👑 **Undisputed** — Win 3 tournaments in a row.
- 🎂 **Veteran** — Play 100+ matches.
- 🌟 **Debutant** — Play your first tournament.
- ⚡ **Speed Demon** — Win a best-of-3 match in under 15 minutes (if time tracking is added).
- 🎪 **Social Butterfly** — Play against 20+ different opponents.

### 7. Daily Slack Summary

**Every day at a configurable time (e.g., 09:00 the next day):**
- Bot posts a summary of matches completed the previous day:
  - Match results with scores.
  - Notable ELO movements (biggest gains/losses).
  - Any new badges earned.
  - Current tournament status (if still in progress).
- On non-tournament days, if there are no results, skip the message (don't spam).

### 8. Slack Bot Commands

| Command | Description |
|---|---|
| `/rakket score @opponent 11-7 9-11 11-5` | Report a match score |
| `/rakket standings` | Show current tournament standings |
| `/rakket leaderboard` | Show top 10 ELO rankings |
| `/rakket profile @user` | Show player stats summary |
| `/rakket next` | Show your next match |
| `/rakket history` | Link to tournament history page |
| `/rakket help` | List available commands |
| `/rakket stats` | Your personal quick stats |

### 9. Additional Creative Features

**Trash Talk Bot** (fun/optional):
- When a match is reported, the bot occasionally drops a witty comment: "🔥 @alice crushed @bob 2-0. Someone call the fire department!" — Generate a few templates with player name placeholders.

**Weekly Recap**:
- Every Friday (or after the last match of the week), post a weekly recap in Slack: tournament winner, biggest ELO mover, most active player, new badges earned.

**Challenge Mode** (future):
- Allow players to challenge each other to friendly (unranked) matches outside tournaments via `/rakket challenge @opponent`. Track these separately.

**Rematch Notifications**:
- When two players with a close head-to-head record (e.g., 5-4) are paired again, the bot hypes it up: "🔥 Rivalry alert! @alice vs @bob — currently tied 5-5 all time!"

---

## Web UI Pages

### Design Direction
- Clean, modern, responsive design. Dark mode support.
- Use Tailwind CSS for styling.
- Real-time feel (polling or WebSocket for live tournament updates).
- Mobile-friendly (people will check on their phones).
- Subtle animations for ELO changes, badge unlocks, match results.

### Pages

1. **Dashboard** — Current/upcoming tournament status, recent results, quick leaderboard top 5, next scheduled tournament countdown.
2. **Tournament (live)** — Swiss rounds visualization, match results, current standings. Live-updating.
3. **Tournament History** — List of past tournaments. Click to see full details.
4. **Leaderboard** — Full rankings with filters (ELO / points / time range).
5. **Player Profile** — Stats, ELO chart, head-to-head, badges, match history.
6. **Admin** — (if user is admin) Manage tournaments, resolve disputes, manually adjust scores, configure settings.

---

## API Endpoints (REST)

```
# Auth
GET  /api/auth/slack           → Slack OAuth flow
GET  /api/auth/callback         → OAuth callback
GET  /api/auth/me               → Current user info
POST /api/auth/logout           → Logout

# Tournaments
GET  /api/tournaments           → List all (paginated, filterable)
GET  /api/tournaments/current   → Current active tournament
GET  /api/tournaments/:id       → Tournament detail with rounds & matches
POST /api/tournaments/:id/score → Report a match score

# Leaderboard
GET  /api/leaderboard           → ?type=elo|points&period=4w|12w|all

# Players
GET  /api/players               → List all players
GET  /api/players/:id           → Player profile + stats
GET  /api/players/:id/matches   → Match history (paginated)
GET  /api/players/:id/h2h       → Head-to-head records
GET  /api/players/:id/badges    → Earned badges
GET  /api/players/:id/elo-history → ELO over time (for chart)

# Admin
POST /api/admin/tournament/create  → Manually trigger tournament
PUT  /api/admin/match/:id/score    → Override a score
PUT  /api/admin/match/:id/dispute  → Resolve a dispute
GET  /api/admin/settings           → Get app settings
PUT  /api/admin/settings           → Update app settings

# Health
GET  /api/health                → Health check (for Docker/k8s)
```

---

## Database Schema (Initial Migration)

Design the PostgreSQL schema to cover:

- **players** — id, slack_user_id, display_name, avatar_url, elo_rating, total_matches, total_wins, created_at, updated_at
- **tournaments** — id, date, status (registration/active/completed/cancelled), slack_message_ts, participant_count, created_at
- **tournament_participants** — tournament_id, player_id, final_placement, points_awarded, rounds_won
- **tournament_rounds** — id, tournament_id, round_number, status (pending/active/completed)
- **matches** — id, round_id, player1_id, player2_id, winner_id, status (pending/completed/disputed/bye/forfeit), reported_by, created_at, completed_at
- **match_sets** — id, match_id, set_number, player1_score, player2_score
- **elo_history** — id, player_id, match_id, elo_before, elo_after, elo_change, timestamp
- **badges** — id, player_id, badge_type, earned_at, match_id (nullable), tournament_id (nullable)
- **schema_version** — version, applied_at, description (for the migration runner)

Add proper indexes for common queries (player lookups, leaderboard sorting, tournament listing by date).

---

## Docker Setup

### Multi-stage Dockerfile

```dockerfile
# Stage 1: Build frontend
FROM node:20-alpine AS frontend-build
WORKDIR /app/frontend
COPY frontend/package*.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

# Stage 2: Build backend
FROM gradle:8-jdk21-alpine AS backend-build
WORKDIR /app/backend
COPY backend/ ./
COPY --from=frontend-build /app/frontend/dist ./src/main/resources/static
RUN gradle shadowJar --no-daemon

# Stage 3: Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=backend-build /app/backend/build/libs/*-all.jar app.jar
COPY db/migrations/ ./db/migrations/
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=3s CMD wget -qO- http://localhost:8080/api/health || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### docker-compose.yml (local dev)

```yaml
version: "3.8"
services:
  app:
    build: .
    ports:
      - "8080:8080"
    env_file: .env
    depends_on:
      postgres:
        condition: service_healthy

  postgres:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: rakket
      POSTGRES_USER: rakket
      POSTGRES_PASSWORD: changeme
    ports:
      - "5432:5432"
    volumes:
      - pgdata:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U rakket"]
      interval: 5s
      timeout: 5s
      retries: 5

volumes:
  pgdata:
```

---

## Implementation Notes

- **Swiss pairing algorithm**: Implement the Dutch system variant — sort players by score, then pair top-to-bottom within score groups, avoiding rematches.
- **Concurrency**: Use Ktor's coroutine support for scheduled tasks (registration message, tournament creation, daily summary).
- **Scheduled tasks**: Use `kotlinx-coroutines` with a simple scheduler for cron-like triggers based on configured times/timezone.
- **Slack events**: Use Slack's Socket Mode (easier for self-hosted, no public URL needed for events).
- **Frontend routing**: React Router for SPA navigation. API proxy in dev mode via Vite.
- **Error handling**: Graceful error messages in Slack. Web UI shows toast notifications.
- **Logging**: SLF4J with structured logging. Log all score reports and ELO changes.
- **Testing**: Include unit tests for the Swiss pairing algorithm, ELO calculator, and score validation. Integration tests for API endpoints.
- **API documentation**: Serve an OpenAPI/Swagger spec at `/api/docs` (optional but nice for open source).

---

## Build Order (Suggested Implementation Sequence)

1. **Project scaffolding** — Monorepo, Gradle/Ktor setup, React/Vite setup, Dockerfile, docker-compose, all open-source files (README, LICENSE, CONTRIBUTING, etc.).
2. **GitHub workflows** — CI pipeline, docs deployment, release pipeline.
3. **Database** — Schema, migration runner, connection pool.
4. **Slack OAuth** — Auth flow, session management, JWT tokens.
5. **Player registration** — Sync Slack users, player model.
6. **Tournament lifecycle** — Scheduled registration message, reaction collection, Swiss bracket generation.
7. **Match scoring** — Slack command + web UI, score validation, set tracking.
8. **ELO system** — Rating calculation, history tracking, K-factor logic.
9. **Leaderboard** — API + frontend page with filters.
10. **Tournament history** — API + frontend page with detail view.
11. **Player profiles** — Stats, head-to-head, ELO chart.
12. **Badges** — Achievement engine, Slack announcements, profile display.
13. **Daily & weekly summaries** — Scheduled Slack messages.
14. **Admin panel** — Dispute resolution, manual overrides, settings.
15. **GitHub Pages docs site** — Landing page, setup guides, API reference.
16. **Polish** — Dark mode, mobile responsive, animations, error handling, final README with screenshots.
