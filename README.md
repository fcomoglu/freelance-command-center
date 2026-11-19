# Freelance Command Center

AI-assisted workspace for freelancers: discover jobs across freelance platforms, score them against your profile,
draft proposals with AI and track every bid — from the web, your phone or your browser.

> Status: early development — the project skeleton is in place; features are being built.

## Features

| Feature | Status |
|---|---|
| **Accounts:** sign-up with e-mail verification, sign-in on web and mobile, password reset | in progress |
| **Unified job feed** from Freelancer.com and Upwork (official APIs) and from Fiverr, Bionluk and Arc.dev (browser extension, inside your own session) | planned |
| **AI fit score** for every job, based on your skills, experience and rates — with the reasons behind the score | planned |
| **Proposal drafts** in the job's language, with a suggested price; you review and approve before anything is sent | planned |
| **Bid tracking** and notifications on web and mobile | planned |
| **Multi-language UI** (English, Turkish) | in progress |

## Tech stack

| Part | Technology |
|---|---|
| Backend | Java 25, Spring Boot 4, Spring Modulith, PostgreSQL, Redis, Flyway |
| AI | Claude via the official Anthropic Java SDK |
| Web | React, TypeScript, Vite, TanStack Query, i18next |
| Mobile | Expo (React Native), TypeScript, Expo Router |
| Extension | Chrome Manifest V3, TypeScript |
| Tooling | Maven, npm workspaces, Docker Compose, GitHub Actions |

## Getting started

Requirements: JDK 25, Node 22+, Docker.

```bash
npm install                           # TypeScript workspaces (web, mobile, shared packages)
cd apps/api && ./mvnw verify          # backend build and tests
cd apps/api && ./mvnw spring-boot:run # API on http://localhost:8080 (starts PostgreSQL, Redis and Mailpit via Docker Compose)
npm run web:dev                       # web app on http://localhost:5173; e-mails arrive in Mailpit, http://localhost:8025
npm run mobile:start                  # mobile app (Expo)
```

The mobile app calls `http://localhost:8080` (Android emulator: `http://10.0.2.2:8080`). On a physical device, set
`EXPO_PUBLIC_API_URL` to your computer's LAN address, e.g. `EXPO_PUBLIC_API_URL=http://192.168.1.20:8080`.

## Documentation

- [Architecture](docs/architecture.md) — system design, modules and data flow.
- [Decision records](docs/adr) — why the key technical decisions were made.
- [CLAUDE.md](CLAUDE.md) — repository layout, conventions and the full command list.
