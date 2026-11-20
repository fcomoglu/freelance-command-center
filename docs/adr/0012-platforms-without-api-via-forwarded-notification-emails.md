# 12. Platforms without an API: forwarded notification e-mails instead of a browser extension

- Status: accepted
- Date: 2026-11-20
- Supersedes: [ADR 0004](0004-platform-integration-via-api-or-user-driven-extension.md)

## Context

ADR 0004 planned a browser extension that reads Fiverr, Bionluk and Arc.dev pages inside the user's own session. The
terms of all three platforms rule this out, independent of whose session runs the code:

- Bionluk (Kullanım Şartları) forbids using robots, spiders or other automatic tools to monitor or copy site content
  without prior written permission.
- Fiverr (Terms of Service) forbids any automatic device or software that accesses the site to monitor, extract, copy
  or collect content.
- Arc.dev (Terms, 7.7) forbids scraping and accessing the platform for monitoring purposes, or by any means other than
  its provided interfaces.

All three send notification e-mails (new buyer requests, messages, job matches) to their users, and those e-mails
belong to the user.

## Decision

- Freelancer.com and Upwork stay on their **official APIs**, with OAuth 2 tokens stored encrypted.
- For Fiverr, Bionluk and Arc.dev, the user **forwards the platform's notification e-mails** to a workspace-specific
  inbound address. The application reads only what arrives there; it never accesses those sites.
- Inbound mail reaches the API as raw MIME over an authenticated webhook. In production a mail routing service
  (for example Cloudflare Email Routing with an Email Worker) receives the mail for the inbound domain and posts it.
- One **mail parser per platform** (strategy) turns a recognized e-mail into the canonical model (job, message,
  notification). The raw e-mail is kept so that improved parsers can re-read it.
- Data from forwarded e-mails is **private** to the workspace that received it.
- The application never stores platform passwords and never submits anything to a platform; replies and proposals
  happen on the platform.

## Consequences

- No access to the three platforms, so no conflict with their terms and nothing that breaks when their pages change.
- Coverage is limited to what the e-mails contain: a notification without details becomes a link to the platform.
- Setup asks the user for one forwarding rule in their mail program; the application shows the address and whether
  mail has arrived.
- A mail parser breaks when a platform changes its e-mail template; unrecognized mails stay stored and visible, and a
  fixed parser processes them again.
- The inbound address is a secret: anyone who knows it can send mail into the workspace. It can be replaced by a new
  one at any time.
