# Freelance Command Center

One inbox for your freelance work: new jobs, messages and notifications from every platform you use, on the web, on
your phone and in your browser. AI job scoring and proposal drafts come next.

> Status: early development — the project skeleton is in place; features are being built.

## Features

| Feature | Status |
|---|---|
| **Accounts:** sign-up with e-mail verification, sign-in on web and mobile, password reset | in progress |
| **Unified job feed** from Freelancer.com and Upwork (official APIs) and from Arc.dev and Bionluk (their notification e-mails, forwarded to your inbox address), with push notifications for new jobs | in progress (Freelancer.com and Upwork feed on web and mobile, keyword job alerts as push notifications on Android, inbox address for forwarded e-mails whose jobs stay private to your workspace; parsers for the Arc.dev and Bionluk e-mails next) |
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
| Tooling | Maven, npm workspaces, Docker Compose, GitHub Actions |

## Getting started

Requirements: JDK 25, Node 22+, Docker.

```bash
npm install                           # TypeScript workspaces (web, mobile, shared packages)
cd apps/api && ./mvnw verify          # backend build and tests
cd apps/api && ./mvnw spring-boot:run # API on http://localhost:8080 (starts PostgreSQL, Redis and Mailpit via Docker Compose)
npm run web:dev                       # web app on http://localhost:5173; e-mails arrive in Mailpit, http://localhost:8025
npm run mobile:start                  # mobile app; press s to switch between the development build and Expo Go
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

### Push notifications on Android

Job alerts reach the phone through the Expo Push Service and Firebase Cloud Messaging. Expo Go cannot receive them;
the app needs a development build with your own Expo and Firebase projects:

1. Create a free Expo account, then in `apps/mobile`: `npx eas-cli@latest login` and
   `npx eas-cli@latest init --id <project id>` after creating a project on expo.dev (or let `init` create one). Set
   `EAS_PROJECT_ID` to the project id; for EAS builds add it as an EAS environment variable
   (`npx eas-cli@latest env:create`).
2. In a Firebase project, add an Android app with the package `com.freelancecommandcenter.app` and download its
   `google-services.json` (git-ignored). Add it as the EAS file environment variable `GOOGLE_SERVICES_JSON`
   (`npx eas-cli@latest env:create`); for local builds set `GOOGLE_SERVICES_JSON` to the path of the file.
3. Create a service account key in Firebase (Project settings, Service accounts) and upload it as the FCM V1 key:
   `npx eas-cli@latest credentials` (Android, Google Service Account, push notifications).
4. Build and install the development build: `npx eas-cli@latest build --profile development --platform android`,
   then open the install link on the phone.
5. Start Metro with `npm run mobile:start`, open the app, and turn on notifications under Settings.

### Forwarded platform e-mails

Fiverr, Bionluk and Arc.dev are read from their notification e-mails
([ADR 0012](docs/adr/0012-platforms-without-api-via-forwarded-notification-emails.md)). Each workspace has an inbox
address, shown on the web app's **Platform e-mails** page; a forwarding rule in your mailbox sends the platforms'
notification e-mails there.

- The platform is recognized by the sender, so the rule must keep the original sender: in Outlook use **Redirect to**
  (**Forward to** sends the mail as yourself); Gmail's forwarding keeps it.
- The platforms only send what you have switched on: keep their e-mail notifications enabled, and keep your gigs and
  listings active (Fiverr sends buyer briefs only for active gigs). Fiverr has no job board, so its e-mails bring
  messages, orders and briefs rather than jobs.
- Jobs read from your e-mails are visible only in your workspace.

- The address domain is `INBOUND_MAIL_DOMAIN`. In production a mail routing service (for example Cloudflare Email
  Routing with an Email Worker) receives mail for that domain and posts each raw message to
  `POST /api/v1/inbound-mail` with the recipient in `X-Envelope-To` and `INBOUND_MAIL_SECRET` as bearer token.
- Locally, set `INBOUND_MAIL_SECRET` in `apps/api/.env` and post a saved `.eml` file with the `curl` command
  from [CLAUDE.md](CLAUDE.md#commands).

## Documentation

- [Architecture](docs/architecture.md) — system design, modules and data flow.
- [Decision records](docs/adr) — why the key technical decisions were made.
- [CLAUDE.md](CLAUDE.md) — repository layout, conventions and the full command list.
