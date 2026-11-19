# Freelance Command Center

AI-assisted workspace for freelancers: discover jobs across freelance platforms, score them against your profile,
draft proposals with AI and track every bid — from the web, your phone or your browser.

> Status: early development. See [docs/architecture.md](docs/architecture.md) for the design.

## Features (planned)

- **Unified job feed** from Freelancer.com and Upwork (official APIs) and from Fiverr, Bionluk and Arc.dev
  (browser extension, inside your own session).
- **AI fit score** for every job, based on your skills, experience and rates — with the reasons behind the score.
- **Proposal drafts** in the job's language, with a suggested price; you review and approve before anything is sent.
- **Bid tracking** and notifications on web and mobile.
- **Multi-language UI** (English, Turkish).

## Tech stack

| Part | Technology |
|---|---|
| Backend | Java 25, Spring Boot 4, Spring Modulith, PostgreSQL, Redis, Flyway |
| AI | Claude via the official Anthropic Java SDK |
| Web | React, TypeScript, Vite, TanStack Query, i18next |
| Mobile | Expo (React Native), TypeScript, Expo Router |
| Extension | Chrome Manifest V3, TypeScript |
| Tooling | Gradle, npm workspaces, Docker Compose, GitHub Actions |

## Repository structure

```
apps/        api (Spring Boot), web, mobile, extension
packages/    shared TypeScript packages (api-client, i18n)
docs/        architecture and decision records (ADRs)
infra/       local and deployment infrastructure
```

## Architecture

A modular monolith with hexagonal modules (`identity`, `profile`, `platform`, `job`, `proposal`, `ai`,
`notification`). Key decisions are documented as ADRs in [docs/adr](docs/adr).

## Getting started

Requirements: JDK 25, Node 22+, Docker.

```bash
npm install                      # TypeScript workspaces (web, mobile, shared packages)
cd apps/api && ./gradlew build   # backend build and tests
cd apps/api && ./gradlew bootRun # API on http://localhost:8080 (starts PostgreSQL and Redis via Docker Compose)
npm run web:dev                  # web app
npm run mobile:start             # mobile app (Expo)
```

The full command list is in [CLAUDE.md](CLAUDE.md#commands).
