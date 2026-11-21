# CLAUDE.md

Guidance for Claude Code (and humans) working in this repository. It describes how the repository works **now**;
keep it current as described in [Documentation and comments](#documentation-and-comments).

## Project

**Freelance Command Center** — one inbox for freelancers who work on several platforms (Freelancer.com, Upwork,
Fiverr, Bionluk, Arc.dev, ...): new jobs, messages and platform notifications in one place, with push notifications;
AI job scoring and proposal drafts come later (see [ADR 0011](docs/adr/0011-product-focus-one-inbox.md)).

Clients: **web** (React) and **mobile** (React Native / Expo). Platforms without a public API are read from the
notification e-mails the user forwards to the application
([ADR 0012](docs/adr/0012-platforms-without-api-via-forwarded-notification-emails.md)).

This is a public portfolio project. Code quality, architecture and documentation are part of the product.

## Repository layout

```
apps/
  api/          Spring Boot backend (Java) — modular monolith, hexagonal architecture
  web/          React + TypeScript + Vite web client
  mobile/       Expo (React Native) + TypeScript mobile client
packages/
  api-client/   Typed API client; `openapi.json` is exported by the API, types are generated from it
  i18n/         Shared translation resources (en, tr) for web and mobile
docs/
  architecture.md   System overview, modules, data flow
  adr/              Architecture Decision Records (one file per decision)
infra/              Docker Compose files (local, production), the deploy hook and the Cloudflare inbound mail Worker
.github/            CI workflow and Dependabot configuration
```

Other notes:
- `apps/mobile/AGENTS.md` contains Expo-specific rules (always add Expo/React Native packages with
  `npx expo install`, check the versioned Expo docs before using an API). Follow it when working in `apps/mobile`.
- The Java base package is `com.freelancecommandcenter`.

## Golden rules

1. **English only** in everything that lives in the repository: code, identifiers, comments, commit messages, docs,
   log messages, API error codes, test names. The only non-English text allowed is inside translation files
   (`packages/i18n/locales/<lang>/*.json` for clients, `apps/api/src/main/resources/messages_<lang>.properties` for
   server-rendered texts such as e-mails; `messages.properties` is English).
2. **No hard-coded user-facing strings** in web or mobile. Every visible string goes through i18n
   (`t('key')`), and every new key is added to **all** supported locales (currently `en` and `tr`) in the same change.
   `en` is the source/fallback locale.
3. **No secrets or personal data in the repository.** Configuration comes from environment variables; commit only
   `.env.example` files with placeholder values. Never commit real user data, CVs, tokens or platform cookies.
4. **Respect platform terms.** Platforms are integrated through official APIs where they exist. Platforms without an
   API are never accessed (no scraping, no browser automation); they are read only from notification e-mails the user
   forwards. The application **never submits anything to a platform without an explicit user action**.
5. **Architecture boundaries are enforced by tests** (Spring Modulith verification + ArchUnit). Do not bypass a
   failing boundary test — fix the design or record a new ADR.
6. Every architectural decision that is hard to reverse gets an ADR in `docs/adr/` (`NNNN-kebab-case-title.md`).

## Backend (`apps/api`)

- Java 25 (LTS), Spring Boot 4, Maven (wrapper `mvnw`). Versions not managed by the Spring Boot parent are
  properties at the top of `pom.xml`.
- **Modular monolith** with Spring Modulith. Each top-level package under `com.freelancecommandcenter` is a module,
  declared in its `package-info.java` with `@ApplicationModule`; `shared` is the open kernel. The module list and
  responsibilities are in [docs/architecture.md](docs/architecture.md#backend-modules).
- Local infrastructure (PostgreSQL 18, Redis 8, Mailpit) is defined once in `infra/compose.yaml`; Spring Boot's
  Docker Compose support starts it on `spring-boot:run`. Tests use Testcontainers with the same image versions.
- The Modulith event publication table (outbox) is created by Flyway `V1__event_publication_registry.sql`.
- The compiler runs with `-Xlint:all` and `failOnWarning`: warnings fail the build.
- Inside each module, **hexagonal layering**:
  - `domain` — entities, value objects, domain services, domain events. No Spring, no JPA annotations.
  - `application` — use case classes (one per use case, the module's inbound API for adapters), outbound ports
    (`port.out` interfaces), transactions, event listeners.
  - `adapter.in.web` — REST controllers + request/response DTOs.
  - `adapter.out.*` — persistence (JPA), platform API clients, Claude client, etc.
- **Pragmatic DDD:** modules are bounded contexts with their own language. Modules with real business rules
  (`identity`, `job`, `messaging`, `matching`, `proposal`) use aggregates (invariants enforced inside, changed only through
  their methods), value objects (immutable records), typed IDs (`JobId`) and domain events. Thin modules
  (`notification`) may stay simpler. One aggregate per transaction; other aggregates react to its events.
- Modules talk to each other through their **published API** (`<module>/api` package) or **domain events**, never by
  reaching into another module's internals.
- Cross-module side effects use events persisted by the Spring Modulith event publication registry (transactional
  outbox). Event handlers must be **idempotent**. There is no external message broker; events that must leave the
  process are externalized through Spring Modulith when needed.
- Authenticated endpoints take the caller as an `AuthenticatedUser` parameter (`shared.security`); every read and
  write of user data is scoped to its `workspaceId`.
- REST: follow the [API conventions](docs/architecture.md#api-conventions) (paths, errors, IDs, pagination,
  JSON formats). The contract is code-first: changing a controller or DTO changes `packages/api-client/openapi.json`
  on the next `verify`; commit it together with the regenerated `schema.d.ts`.
- Persistence: PostgreSQL, schema migrations with Flyway only (`V<n>__description.sql`), never `ddl-auto=update`.
  Each module owns a Postgres schema named after it (`identity.users`); no module reads another module's tables.
  JPA entities live in `adapter.out.persistence` and map to and from domain objects.
- AI: official Anthropic Java SDK behind an `ai` module port, so prompts/models are swappable and testable.
  Prompts live in versioned resource files, not string literals in code.
- Tests: JUnit 5, AssertJ, Testcontainers (PostgreSQL, Redis, Mailpit). Unit tests for domain and application,
  end-to-end HTTP tests for adapters, Modulith `ApplicationModules.verify()` and ArchUnit layering tests. Integration
  tests are annotated with `@IntegrationTest` (one shared context and set of containers) and use `ApiClient` for HTTP
  calls and `Mailbox` to read delivered e-mails.
- Test class names decide the phase: `*Test` / `*Tests` run in `test` (Surefire, no Docker needed), `*IT` run in
  `integration-test` (Failsafe, Testcontainers).

## Frontend (`apps/web`, `apps/mobile`)

- npm workspaces (root `package.json`); shared packages are consumed as TypeScript source (no build step).
- TypeScript `strict`, Prettier (root `.prettierrc.json`), oxlint (web), ESLint via `expo lint` (mobile).
- Tests: Vitest for web and the shared packages (web screens with jsdom and Testing Library), Jest with `jest-expo`
  and React Native Testing Library for mobile. Test files sit next to the code as `*.test.ts(x)`; mobile screen tests
  live in `src/__tests__/`, because `src/app/` holds only routes. Screens render through `renderScreen` from
  `src/test-support.tsx`. Tests mock the API client, not `fetch`, except the API client's own tests. The web app's
  React range includes the version mobile pins, so the workspace holds one React copy, which the tests need.
- Server state with TanStack Query; API calls only through `packages/api-client` (`createApiClient`). Never edit
  `openapi.json` or `src/schema.d.ts` by hand.
- Sessions: `createApiClient` also returns a `session` that keeps the access token in memory, refreshes it before it
  expires (one refresh at a time) and ends the session on a rejected token. Screens never handle tokens themselves.
- API errors: show `t(errorKey(problem.code))` from `@fcc/i18n`; every error code the API can return has a key under
  `errors.` in all locales (checked by the API's `ErrorCodeTranslationsTest`).
- i18n with i18next / react-i18next; resources from `packages/i18n`. Language detection: user setting → device/browser
  language → `en`.
- Web: TanStack Router with code-based routes and guards in `src/router.tsx`; screens in `src/pages/` export only
  components (fast refresh). Tailwind CSS v4; shared primitives in `src/components/ui.tsx`. Vite proxies `/api` to
  the local API, so the web app and the API share one origin and the refresh token stays in its HttpOnly cookie.
- Mobile: Expo + Expo Router; secure token storage with `expo-secure-store`.

## Conventions

- Commits: Conventional Commits (`feat(api): ...`, `fix(web): ...`, `docs: ...`, `chore: ...`).
- Branches: `main` is always releasable; work on `feat/...`, `fix/...` branches, merge via pull request.
- Formatting is automated (Spotless for Java, Prettier for TS); do not hand-format against the tools.
- Naming: Java classes in `PascalCase`, use cases named as actions (`ScoreJobUseCase`), ports as roles
  (`JobRepository`, `JobSource`), REST DTOs suffixed `Request` / `Response`.

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
| Mobile: Android development build (EAS, needed for push) | `cd apps/mobile && npx eas-cli@latest build --profile development --platform android` |
| Translation key check | `npm run i18n:check` |
| Regenerate API types from the committed spec | `npm run api:generate` |
| Update the committed spec (`packages/api-client/openapi.json`) | `cd apps/api && ./mvnw verify` |
| Client tests (all, or one workspace) | `npm test` / `npm test -w apps/web` |
| Lint / type-check / format all clients | `npm run lint` / `npm run typecheck` / `npm run format` |
| Infrastructure only | `docker compose -f infra/compose.yaml up -d` |
| Build the container images | `docker build -t fcc-api apps/api` / `docker build -f apps/web/Dockerfile -t fcc-web .` |
| Run the images locally (http://localhost:8081) | `docker compose -f infra/compose.yaml -f infra/compose.app.yaml up --build` |
| Production stack (on the host; settings in `infra/.env` and `apps/api/.env`) | `docker compose -f infra/compose.prod.yaml up -d --build` |
| Deploy `main` to production (remote `prod` = the host's `fcc.git`, see docs/architecture.md#production) | `git push prod main` |
| Deploy the inbound mail Worker (Cloudflare) | `cd infra/cloudflare/inbound-mail-worker && npx wrangler@latest deploy` |
| Local e-mail inbox (Mailpit) | `http://localhost:8025` |
| Deliver a forwarded platform e-mail locally (`INBOUND_MAIL_SECRET` set in `apps/api/.env`) | `curl -H "Authorization: Bearer <secret>" -H "X-Envelope-To: <inbox address>" -H "Content-Type: message/rfc822" --data-binary @mail.eml http://localhost:8080/api/v1/inbound-mail` |

CI (`.github/workflows/ci.yml`) runs the API build, the client checks (i18n, format, lint, type-check, tests, web
build) and builds both container images on every push to `main` and every pull request.
