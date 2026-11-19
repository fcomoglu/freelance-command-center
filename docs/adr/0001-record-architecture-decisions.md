# 1. Record architecture decisions

- Status: accepted
- Date: 2026-11-19

## Context

Decisions about structure, technology and integration shape this project for a long time and are hard to reverse.
They should be easy to find and understand later, by the author, contributors and reviewers of this portfolio project.

## Decision

Use Architecture Decision Records in `docs/adr/`, numbered sequentially (`NNNN-kebab-case-title.md`), each with
Status, Date, Context, Decision and Consequences. A superseded ADR is not deleted; its status points to the new one.

## Consequences

- New hard-to-reverse decisions require a short ADR in the same pull request.
- `CLAUDE.md` links to the ADR folder so AI assistants follow the recorded decisions.
