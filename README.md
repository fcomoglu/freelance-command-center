# Freelance Command Center

One inbox for your freelance work: new jobs, messages and notifications from every platform you use, on the web, on
your phone and in your browser. AI job scoring and proposal drafts come next.

> Status: early development — the project skeleton is in place; features are being built.

## Features

| Feature | Status |
|---|---|
| **Accounts:** sign-up with e-mail verification, sign-in on web and mobile, password reset | in progress |
| **Unified job feed** from Freelancer.com and Upwork (official APIs) and from Fiverr, Bionluk and Arc.dev (browser extension, inside your own session), with push notifications for new jobs | in progress (Freelancer.com and Upwork feed on web and mobile; push notifications and the extension platforms next) |
| **Unified messages:** conversations from every platform in one list, with unread state and push notifications | planned |
| **Bid and platform notifications:** bid status changes and platform notices in one feed | planned |
| **AI fit score** for every job, based on the profile imported from your platform accounts, with the reasons behind the score | planned |
| **Proposal drafts** in the job's language, with a suggested price; you review and approve before anything is sent | planned |
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

### Platform credentials

The job sync reads each platform with credentials from `apps/api/.env` (see `apps/api/.env.example`); a platform
without credentials is left out.

- **Freelancer.com:** generate a personal access token at https://accounts.freelancer.com/settings/develop and set
  `FREELANCER_ACCESS_TOKEN`. It is valid for 30 days.
- **Upwork:** request an API key at https://www.upwork.com/developer/keys/apply with the permission "Read marketplace
  Job Postings" and set `UPWORK_CLIENT_ID` and `UPWORK_CLIENT_SECRET`. Then authorize the key once:
  1. Open `https://www.upwork.com/ab/account-security/oauth2/authorize?response_type=code&client_id=<client id>&redirect_uri=<callback URL of the key>`
     and allow access; the browser is sent to the callback URL with a `code` parameter.
  2. Exchange the code for tokens:
     `curl -X POST https://www.upwork.com/api/v3/oauth2/token -d grant_type=authorization_code -d client_id=<client id> -d client_secret=<secret> -d redirect_uri=<callback URL> -d code=<code>`
  3. Set `UPWORK_REFRESH_TOKEN` to the `refresh_token` of the answer. The API renews access tokens itself and stores
     every newer refresh token; authorize again only when Upwork rejects it (unused for two weeks).

The mobile app calls `http://localhost:8080` (Android emulator: `http://10.0.2.2:8080`). On a physical device, set
`EXPO_PUBLIC_API_URL` to your computer's LAN address, e.g. `EXPO_PUBLIC_API_URL=http://192.168.1.20:8080`.

## Documentation

- [Architecture](docs/architecture.md) — system design, modules and data flow.
- [Decision records](docs/adr) — why the key technical decisions were made.
- [CLAUDE.md](CLAUDE.md) — repository layout, conventions and the full command list.
