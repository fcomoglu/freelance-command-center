# 4. Platform integration: official APIs first, user-driven extension otherwise

- Status: superseded by [ADR 0012](0012-platforms-without-api-via-forwarded-notification-emails.md)
- Date: 2026-11-19

## Context

Freelancer.com and Upwork offer official APIs. Fiverr, Bionluk and Arc.dev do not offer a public API for these use
cases. Server-side scraping or headless automation of those sites would require storing user credentials, is fragile,
and conflicts with their terms of service.

## Decision

- Use the **official API** wherever one exists (Freelancer.com REST, Upwork GraphQL), with OAuth 2 tokens stored
  encrypted.
- For platforms without an API, use a **browser extension** that runs inside the user's own logged-in browser session:
  it reads listings the user can see and sends normalized data to the API, and it pre-fills proposal forms.
- The extension **never submits** a proposal, message or bid by itself; the final action is always a user click.
- The application never stores platform passwords.

## Consequences

- No credential storage for scraped platforms; lower legal and security risk.
- Coverage for extension-based platforms depends on the user having the browser open; mobile users get those jobs once
  the extension has synced them.
- Each platform adapter implements the same ports, so adding a platform does not change the core modules.
