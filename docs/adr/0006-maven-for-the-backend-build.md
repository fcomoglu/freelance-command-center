# 6. Maven for the backend build

- Status: accepted
- Date: 2026-11-19
- Supersedes: the backend build tool in [ADR 0003](0003-monorepo-and-client-stack.md)

## Context

The backend is a single Spring Boot module with a standard build: compile, tests, formatting, coverage and a
runnable jar. It does not need a programmable build. The main maintainer works with Maven day to day, and Maven is
the build tool most Java reviewers read without effort.

## Decision

- Build `apps/api` with **Maven**, through the Maven Wrapper (`mvnw`) so the Maven version is pinned in the
  repository.
- Inherit from `spring-boot-starter-parent`; versions it does not manage are properties at the top of `pom.xml`.
- Unit tests (`*Test`, `*Tests`) run with Surefire, integration tests (`*IT`) with Failsafe; `verify` runs both,
  the Spotless format check and the JaCoCo report.

## Consequences

- The build is declarative and predictable; plugin behavior follows Maven's fixed lifecycle.
- Incremental build and build cache features of Gradle are not available; for a single module the difference is
  small.
- The TypeScript side keeps npm workspaces; the two toolchains stay independent.
