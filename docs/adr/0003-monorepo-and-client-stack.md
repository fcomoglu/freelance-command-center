# 3. Monorepo with React web, Expo mobile and a Chrome extension

- Status: accepted; the backend build tool is superseded by [ADR 0006](0006-maven-for-the-backend-build.md), the
  browser extension by [ADR 0012](0012-platforms-without-api-via-forwarded-notification-emails.md)
- Date: 2026-11-19

## Context

The product needs a web client, a mobile client (iOS and Android) and a browser extension. All three call the same
API and show the same texts in several languages.

## Decision

- One repository: backend in `apps/api`, clients in `apps/web`, `apps/mobile`, `apps/extension`, shared TypeScript
  packages in `packages/`.
- TypeScript everywhere on the client side: React + Vite (web), Expo / React Native (mobile), Chrome Manifest V3
  (extension).
- npm workspaces for the TypeScript projects; Gradle for the backend.
- `packages/api-client` is generated from the backend OpenAPI specification so all clients share typed API access.
- `packages/i18n` holds the translation resources shared by all clients.

## Consequences

- A change to the API contract and its client usage can land in one pull request.
- Shared code (API client, translations, types) has a single source.
- CI must run per-app jobs so a change to one app does not rebuild everything unnecessarily.
