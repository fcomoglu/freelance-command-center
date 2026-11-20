# 14. Platform account connections through OAuth, with encrypted tokens and polling

- Status: accepted
- Date: 2026-11-20

## Context

ADR 0013 reads messages from Freelancer.com and Upwork through each workspace's own platform account. That needs
an OAuth 2 connection per workspace: the user grants access on the platform, and the application keeps tokens that
act as that user on the platform until they expire or are revoked. These are the most sensitive values the
application stores.

Freelancer.com reviews every OAuth app before it can be used (about five business days) and refuses apps that serve a
single user. Neither platform offers push delivery of new messages to the application.

## Decision

- **Authorization code flow through the web client.** `POST /api/v1/platform-connections/{platform}/authorizations`
  returns the platform's consent page with a random `state`. The platform sends the user back to the web client
  (`/connections/{platform}/callback`), which posts `code` and `state` to the API as the signed-in user. The API
  stores the state for 15 minutes, uses it once, and accepts it only from the workspace that started it, so a code
  cannot be planted into another workspace. The callback lives in the web client because the API's own session is a
  bearer token that a browser redirect does not carry.
- **One connection per workspace and platform.** Connecting again replaces it; disconnecting deletes its tokens. The
  application cannot withdraw the access on the platform; the user does that in the platform's settings.
- **Tokens are encrypted by the application** with AES-256-GCM and a key from the environment
  (`PLATFORM_TOKEN_KEY`), so a database dump or backup alone does not give access to anyone's platform account. The
  API refuses to start with a configured OAuth app but no key. Tokens never appear in logs or API responses.
- **Messages are polled.** A scheduled read (every 2 minutes by default) asks each active connection for messages
  since the newest one read, renews tokens a day before they expire, and publishes the messages as `MessageReceived`
  with full detail. A connection whose tokens the platform refuses is marked `reconnect_required` and no longer read
  until the user connects again.
- Each platform implements the flow as an `AccountConnector` and the read as a `MessageSource` (ADR 0010); a platform
  is connectable only when its OAuth app is configured on the server.

## Consequences

- Connections cannot be tried against a platform before its OAuth app is approved; tests use fakes of both
  capabilities, and the adapters are tested against recorded responses.
- Losing or changing `PLATFORM_TOKEN_KEY` makes every stored token unreadable, so all accounts must be connected
  again. Rotating the key without that requires re-encrypting the tokens (planned when needed).
- New messages reach the inbox within the polling interval, not instantly, and each connection costs one request per
  interval even when nothing changed.
- The web client is the only place to connect an account; the mobile app shows conversations from connected accounts
  but does not start connections.
