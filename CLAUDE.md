# CLAUDE.md

Guidance for Claude Code (and humans) working in this repository. It describes how the repository works **now**;
keep it current as described in [Documentation and comments](#documentation-and-comments).

## Project

**Freelance Command Center** — an AI-assisted application that lets freelancers manage their work across multiple
freelance platforms (Freelancer.com, Upwork, Fiverr, Bionluk, Arc.dev, ...) from one place: discover and score jobs
against the freelancer's profile, draft proposals with AI, track bids, and get notified.

Clients: **web** (React), **mobile** (React Native / Expo) and a **browser extension** (Chrome, MV3) for platforms
that do not offer a public API.

This is a public portfolio project. Code quality, architecture and documentation are part of the product.

## Repository layout

```
apps/
  api/          Spring Boot backend (Java) — modular monolith, hexagonal architecture
  web/          React + TypeScript + Vite web client
  mobile/       Expo (React Native) + TypeScript mobile client
  extension/    Chrome MV3 extension (TypeScript) for platforms without an API (planned)
packages/
  api-client/   TypeScript API client generated from the backend OpenAPI spec (planned)
  i18n/         Shared translation resources (en, tr) for web, mobile and extension
docs/
  architecture.md   System overview, modules, data flow
  adr/              Architecture Decision Records (one file per decision)
infra/              Docker Compose files
.github/            CI workflow and Dependabot configuration
```

Other notes:
- `apps/mobile/AGENTS.md` contains Expo-specific rules (always add Expo/React Native packages with
  `npx expo install`, check the versioned Expo docs before using an API). Follow it when working in `apps/mobile`.
- The Java base package is `dev.furkancomoglu.fcc`.

## Golden rules

1. **English only** in everything that lives in the repository: code, identifiers, comments, commit messages, docs,
   log messages, API error codes, test names. The only non-English text allowed is inside translation files
   (`packages/i18n/locales/<lang>/*.json`).
2. **No hard-coded user-facing strings** in web, mobile or extension. Every visible string goes through i18n
   (`t('key')`), and every new key is added to **all** supported locales (currently `en` and `tr`) in the same change.
   `en` is the source/fallback locale.
3. **No secrets or personal data in the repository.** Configuration comes from environment variables; commit only
   `.env.example` files with placeholder values. Never commit real user data, CVs, tokens or platform cookies.
4. **Respect platform terms.** Platforms are integrated through official APIs where they exist. Where there is no API,
   the browser extension acts only inside the user's own logged-in session and **never submits anything without an
   explicit user action** (the user clicks the final send/submit button).
5. **Architecture boundaries are enforced by tests** (Spring Modulith verification + ArchUnit). Do not bypass a
   failing boundary test — fix the design or record a new ADR.
6. Every architectural decision that is hard to reverse gets an ADR in `docs/adr/` (`NNNN-kebab-case-title.md`).

## Backend (`apps/api`)

- Java 25 (LTS), Spring Boot 4, Maven (wrapper `mvnw`). Versions not managed by the Spring Boot parent are
  properties at the top of `pom.xml`.
- **Modular monolith** with Spring Modulith. Top-level packages under `dev.furkancomoglu.fcc` are modules:
  `identity`, `profile`, `platform`, `job`, `proposal`, `ai`, `notification`, and the open `shared` kernel. Each
  module is declared in its `package-info.java` with `@ApplicationModule`.
- Local infrastructure (PostgreSQL 18, Redis 8) is defined once in `infra/compose.yaml`; Spring Boot's Docker Compose
  support starts it on `spring-boot:run`. Tests use Testcontainers with the same image versions.
- The Modulith event publication table (outbox) is created by Flyway `V1__event_publication_registry.sql`.
- The compiler runs with `-Xlint:all` and `failOnWarning`: warnings fail the build.
- Inside each module, **hexagonal layering**:
  - `domain` — entities, value objects, domain services, domain events. No Spring, no JPA annotations.
  - `application` — use cases (one class per use case), ports (`port.in` / `port.out` interfaces), transactions.
  - `adapter.in.web` — REST controllers + request/response DTOs.
  - `adapter.out.*` — persistence (JPA), platform API clients, Claude client, etc.
- Modules talk to each other through their **published API** (`<module>/api` package) or **domain events**, never by
  reaching into another module's internals.
- Cross-module side effects use events persisted by the Spring Modulith event publication registry (transactional
  outbox). Event handlers must be **idempotent**.
- REST: resource-oriented, `/api/v1/...`, OpenAPI via springdoc; errors as RFC 9457 Problem Details with a stable
  `code` field that clients translate.
- Persistence: PostgreSQL, schema migrations with Flyway only (`V<n>__description.sql`), never `ddl-auto=update`.
- AI: official Anthropic Java SDK behind an `ai` module port, so prompts/models are swappable and testable.
  Prompts live in versioned resource files, not string literals in code.
- Tests: JUnit 5, AssertJ, Testcontainers (PostgreSQL, Redis). Unit tests for domain/application, slice or
  integration tests for adapters, Modulith `ApplicationModules.verify()` test.
- Test class names decide the phase: `*Test` / `*Tests` run in `test` (Surefire, no Docker needed), `*IT` run in
  `integration-test` (Failsafe, Testcontainers).

## Frontend (`apps/web`, `apps/mobile`, `apps/extension`)

- npm workspaces (root `package.json`); shared packages are consumed as TypeScript source (no build step).
- TypeScript `strict`, Prettier (root `.prettierrc.json`), oxlint (web), ESLint via `expo lint` (mobile).
  Tests: Vitest (web, extension) and Jest (mobile, Expo default) (planned).
- Server state with TanStack Query; API calls only through `packages/api-client` (generated, do not edit by hand).
- i18n with i18next / react-i18next; resources from `packages/i18n`. Language detection: user setting → device/browser
  language → `en`.
- Mobile: Expo + Expo Router; secure token storage with `expo-secure-store`.

## Conventions

- Commits: Conventional Commits (`feat(api): ...`, `fix(web): ...`, `docs: ...`, `chore: ...`).
- Branches: `main` is always releasable; work on `feat/...`, `fix/...` branches, merge via pull request.
- Formatting is automated (Spotless for Java, Prettier for TS); do not hand-format against the tools.
- Naming: Java classes in `PascalCase`, use cases named as actions (`ScoreJobUseCase`), ports as roles
  (`JobRepository`, `PlatformJobSource`), REST DTOs suffixed `Request` / `Response`.

## Documentation and comments

Documentation describes the **current state** of the project, written in the present tense ("The API issues JWT
access tokens"), never as a history ("We changed X to Y", "previously", "now", "new"). History lives in git commits;
the reasons behind decisions live in ADRs.

Each fact has one home; other files link to it instead of repeating it:

| File | Owns |
|---|---|
| `README.md` | What the product is, feature list with status, tech stack, quick start, links to the docs below. |
| `CLAUDE.md` | Working rules: layout, golden rules, conventions, the full command list. |
| `docs/architecture.md` | The system design: context, modules, flows, integration, AI cost, i18n. |
| `docs/adr/` | Why a hard-to-reverse decision was made. The only history-style documents. |
| `apps/<app>/AGENTS.md` | Rules specific to one app (only where needed). |

Rules:
- Update the affected docs **in the same commit** as the change. Rewrite the outdated text; do not append notes about
  what changed.
- Remove text that is no longer true. Mark not-yet-built parts with `(planned)` and drop the marker when they ship.
- Keep it short: facts, rules and reasons a reader cannot get from the code. No restating of what the code shows.
- Accepted ADRs are not edited; a new ADR supersedes an old one and the old one's status links to it.

Code comments follow the same principle:
- Explain **why** (intent, constraints, non-obvious trade-offs), not what the code does. Prefer a clearer name over a
  comment.
- One-line doc comments on public types and module entry points; longer only when the contract is not obvious.
- No history in comments ("changed to", "old version", "fix for ..."), no commented-out code, no TODOs (track work
  outside the code).

## Commands

Every command added to the project must be listed here. Requirements: JDK 25, Node 22+, Docker (for running the API
locally and for integration tests; integration tests are skipped when Docker is not available).

| Task | Command (from the repository root unless noted) |
|---|---|
| Install TypeScript dependencies | `npm install` |
| API: build + format check + all tests | `cd apps/api && ./mvnw verify` (`mvnw.cmd` in PowerShell/cmd) |
| API: unit tests only | `cd apps/api && ./mvnw test` |
| API: auto-format Java | `cd apps/api && ./mvnw spotless:apply` |
| API: run locally (starts `infra/compose.yaml`) | `cd apps/api && ./mvnw spring-boot:run` |
| API: run with Testcontainers instead of Compose | `cd apps/api && ./mvnw spring-boot:test-run` |
| API docs (when running) | `http://localhost:8080/api/docs` (Swagger UI), `/api/openapi` (spec) |
| Module documentation (generated by tests) | `apps/api/target/spring-modulith-docs/` |
| Test coverage report (after `verify`) | `apps/api/target/site/jacoco/index.html` |
| Web: dev server | `npm run web:dev` |
| Web: production build | `npm run web:build` |
| Mobile: start Expo | `npm run mobile:start` |
| Mobile: add a package | `cd apps/mobile && npx expo install <package>` |
| Translation key check | `npm run i18n:check` |
| Lint / type-check / format all clients | `npm run lint` / `npm run typecheck` / `npm run format` |
| Infrastructure only | `docker compose -f infra/compose.yaml up -d` |

CI (`.github/workflows/ci.yml`) runs the API build and the client checks (i18n, format, lint, type-check, web build)
on every push to `main` and every pull request.
