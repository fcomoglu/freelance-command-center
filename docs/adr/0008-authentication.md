# 8. Authentication with own JWT access and rotating refresh tokens

- Status: accepted
- Date: 2026-11-19

## Context

Web, mobile and the browser extension call the same API. Authentication must be cheap to run, safe against token
theft in the browser, and open to social sign-in later. Options were an own implementation in the `identity` module,
a self-hosted identity server (Keycloak) and a managed service (Auth0, Clerk, Cognito).

## Decision

- Implement authentication in the `identity` module with Spring Security; no separate identity server.
- E-mail and password sign-in, Argon2id password hashing, e-mail verification and password reset. Sign-in with Google
  is added later through the OAuth 2 client support and attaches to the same user.
- Short-lived RS256 JWT access tokens (15 minutes) and opaque, hashed, rotating refresh tokens (30 days) with reuse
  detection per token family.
- Web keeps the refresh token in an `HttpOnly`, `Secure`, `SameSite=Strict` cookie scoped to `/api/v1/auth` and the
  access token in memory; mobile stores both in `expo-secure-store`.
- E-mail is sent through a port; local development uses Mailpit, the production provider is chosen at deployment.

## Consequences

- No extra service or per-user cost; the whole flow is visible and tested in this repository.
- The project owns security-sensitive code: key management, token rotation, rate limiting and their tests.
- JavaScript can never read the refresh token, so an XSS flaw cannot steal a long-lived session; CSRF exposure is
  limited to the refresh endpoint, which `SameSite=Strict` and an origin check cover.
- MFA, if needed, is built in `identity` later.
