# 🏓 Rakket

[![CI](https://github.com/YOUR_USER/rakket/actions/workflows/ci.yml/badge.svg)](https://github.com/YOUR_USER/rakket/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Docker](https://img.shields.io/badge/docker-ghcr.io-blue)](https://github.com/YOUR_USER/rakket/pkgs/container/rakket)

**Your office ping pong league, automated.**

> A self-hosted, Slack-integrated ping pong tournament manager with ELO rankings, Swiss-system brackets, achievements, and a beautiful web dashboard. Built with Kotlin/Ktor + React + PostgreSQL. Ships as a single Docker image.

<!-- Screenshot placeholder -->
<!-- ![Rakket Dashboard](docs/assets/screenshot.png) -->

## Features

- 🗓️ **Automated weekly tournaments** — Slack bot handles registration, bracket generation, and results
- 🏆 **Swiss-system brackets** — Fair matchups using the Dutch pairing variant
- 📊 **ELO rankings** — Track skill progression with a proper rating system
- 🎖️ **Achievements & badges** — 16+ unlockable badges for milestones and feats
- 📈 **Player profiles** — Stats, ELO charts, head-to-head records, win streaks
- 💬 **Slack-first** — Score reporting, standings, and notifications right in Slack
- 🌐 **Web dashboard** — Beautiful, responsive UI for leaderboards, history, and profiles
- 🔒 **Slack OAuth** — Only workspace members can access the web UI
- 🐳 **Single Docker image** — Easy self-hosting with Docker Compose
- 🌙 **Dark mode** — Because we're civilized

## Quick Start

```bash
# 1. Clone the repo
git clone https://github.com/YOUR_USER/rakket.git
cd rakket

# 2. Configure environment
cp .env.example .env
# Edit .env with your Slack credentials and preferences

# 3. Start everything
docker-compose up -d
```

The app will be available at `http://localhost:8080`.

## Slack Setup

1. Create a new Slack app at [api.slack.com/apps](https://api.slack.com/apps)
2. Enable **Socket Mode** for easy self-hosting
3. Add the required bot scopes: `chat:write`, `reactions:read`, `commands`, `users:read`
4. Install the app to your workspace
5. Copy the bot token and signing secret to your `.env` file

See the full [Slack Setup Guide](docs/slack-setup.md) for detailed instructions.

## Tech Stack

| Layer | Technology |
|---|---|
| Backend | Kotlin + Ktor |
| Frontend | React (TypeScript) + Tailwind CSS |
| Database | PostgreSQL |
| Slack | Slack Bolt SDK for JVM |
| Auth | Slack OAuth 2.0 |
| Packaging | Single Docker image (multi-stage build) |

## Configuration

All configuration is done via environment variables. See [`.env.example`](.env.example) for the full reference, or check the [Configuration docs](docs/configuration.md).

## Contributing

Contributions are welcome! Please read [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines on how to get started.

## Repository Settings

We recommend enabling the following on your GitHub repository:

- **Discussions** — for community Q&A
- **Projects** — for roadmap tracking
- **Topics**: `ping-pong`, `table-tennis`, `tournament`, `slack-bot`, `kotlin`, `ktor`, `react`, `docker`, `elo-rating`, `swiss-system`, `open-source`

## License

[MIT](LICENSE)

---

Built with ☕ and 🏓
