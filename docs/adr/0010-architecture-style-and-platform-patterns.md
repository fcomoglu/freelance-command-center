# 10. Architecture style and platform integration patterns

- Status: accepted
- Date: 2026-11-19

## Context

ADR 0002 set a modular monolith with hexagonal modules. Two questions remained: how far to apply DDD and
event-driven architecture, and how to structure integrations with platforms that differ widely in what they allow
(API job search, API bid submission, browser-only access).

## Decision

- **Hexagonal** naming and dependency rule inside modules (`domain`, `application`, `adapter`); Onion and Clean
  Architecture share the same rule and are not used as separate vocabularies.
- **Pragmatic DDD:** strategic DDD everywhere (modules are bounded contexts, `platform` is the anti-corruption layer);
  tactical patterns (aggregates, value objects, typed IDs, domain events) in modules with real business rules only.
- **In-process event-driven architecture:** modules communicate through domain events stored in the Spring Modulith
  outbox, with idempotent handlers. No message broker and no event sourcing; history that must be auditable (proposal
  status changes) is stored as plain history records.
- **Platform integration:** the Strategy pattern over small capability ports, selected through a registry, with a
  per-platform translator into the canonical model and decorators for cross-cutting concerns.

## Consequences

- A new platform is one adapter plus its translator; core modules do not change.
- Platforms without a capability simply do not implement that port, so callers check capabilities instead of
  catching unsupported-operation errors.
- Tactical DDD costs ceremony only where it protects business rules.
- Moving events to RabbitMQ or Kafka later is a configuration change in Spring Modulith, not a redesign; until then
  there is no broker to operate.
