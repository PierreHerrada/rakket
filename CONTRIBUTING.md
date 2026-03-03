# Contributing to Rakket

Thank you for your interest in contributing to Rakket! This guide will help you get started.

## Development Environment Setup

### Prerequisites

- **JDK 21** (Eclipse Temurin recommended)
- **Node.js 20+** and npm
- **Docker** and Docker Compose
- **PostgreSQL 16** (or use the Docker Compose setup)

### Getting Started

```bash
# Clone the repo
git clone https://github.com/YOUR_USER/rakket.git
cd rakket

# Start the database
docker-compose up postgres -d

# Backend
cd backend
./gradlew build
./gradlew run

# Frontend (in a separate terminal)
cd frontend
npm install
npm run dev
```

The frontend dev server runs on `http://localhost:5173` and proxies API requests to the backend on port 8080.

## Code Style

### Kotlin (Backend)

- Follow the [Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html)
- Use **ktlint** for formatting: `./gradlew ktlintCheck`
- Auto-fix: `./gradlew ktlintFormat`

### TypeScript/React (Frontend)

- Use **ESLint** and **Prettier** for linting and formatting
- Run `npm run lint` to check
- Run `npm run lint:fix` to auto-fix

## Branch Naming

Use the following prefixes:

- `feature/` — New features (e.g., `feature/player-profiles`)
- `fix/` — Bug fixes (e.g., `fix/elo-calculation`)
- `docs/` — Documentation changes (e.g., `docs/api-reference`)
- `chore/` — Maintenance tasks (e.g., `chore/update-dependencies`)

## Commit Messages

Follow [Conventional Commits](https://www.conventionalcommits.org/):

```
feat: add player profile page
fix: correct ELO calculation for new players
docs: update Slack setup guide
chore: bump Ktor to 2.x.x
test: add Swiss pairing unit tests
refactor: extract tournament state machine
```

## Pull Request Process

1. **Create an issue first** for non-trivial changes.
2. **Fork and branch** from `main` using the naming convention above.
3. **Write tests** for new functionality.
4. **Update documentation** if your change affects user-facing behavior.
5. **Update CHANGELOG.md** under `[Unreleased]`.
6. **Submit your PR** with a clear description:
   - What changed and why
   - Link to the related issue
   - Screenshots for UI changes
7. **Address review feedback** promptly.

## Running Tests

```bash
# Backend tests
cd backend
./gradlew test

# Frontend tests
cd frontend
npm test
```

## Issue & PR Labels

| Label | Description |
|---|---|
| `bug` | Something isn't working |
| `feature` | New feature request |
| `docs` | Documentation improvements |
| `good first issue` | Good for newcomers |
| `help wanted` | Extra attention needed |
| `priority: high` | High priority |
| `priority: low` | Low priority |
| `wontfix` | Won't be addressed |

## Questions?

Open a [Discussion](https://github.com/YOUR_USER/rakket/discussions) for questions or ideas.
