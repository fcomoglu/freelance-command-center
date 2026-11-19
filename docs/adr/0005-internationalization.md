# 5. Internationalization: English source, shared client translations

- Status: accepted
- Date: 2026-11-19

## Context

The product targets an international audience and Turkish users first. More languages will follow.

## Decision

- Everything in the repository (code, comments, docs, logs, error codes) is in English.
- User-facing texts are translated; initial locales are `en` (default and fallback) and `tr`.
- Clients use i18next with resources from `packages/i18n/locales/<lang>/<namespace>.json`.
- The API returns stable, language-neutral error codes in RFC 9457 Problem Details; clients map codes to texts.
  Server-rendered texts (emails, push notifications) use Spring `MessageSource` with the user's preferred locale.
- A CI check fails when a translation key exists in one locale but not in another.

## Consequences

- Adding a language means adding one locale folder plus backend message bundles.
- No user-facing string can be hard-coded in a component; reviews and lint rules enforce this.
