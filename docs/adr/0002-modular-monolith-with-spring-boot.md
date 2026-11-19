# 2. Modular monolith with Spring Boot and hexagonal modules

- Status: accepted
- Date: 2026-11-19

## Context

The backend has several distinct concerns (identity, platform integrations, jobs, proposals, AI, notifications) but a
single developer and a small expected load at the start. Microservices would add operational cost (deployment,
networking, distributed data) without a matching benefit.

## Decision

- Build one deployable **Spring Boot** application (Java 25, Spring Boot 4) structured as a **modular monolith** with
  **Spring Modulith**.
- Each top-level package is a module with a small published API; module internals are package-private where possible.
- Inside a module use **hexagonal architecture**: `domain`, `application` (use cases + ports), `adapter.in` /
  `adapter.out`.
- Cross-module side effects go through domain events persisted by the Modulith event publication registry
  (transactional outbox); handlers are idempotent.
- Boundaries are verified in tests (`ApplicationModules.verify()` and ArchUnit rules).

## Consequences

- Simple to run and deploy (one container plus PostgreSQL and Redis).
- Modules can be extracted into services later if a real need appears, because their boundaries are already explicit.
- Slightly more ceremony (ports, adapters, events) than a layered CRUD app; accepted for maintainability and testability.
