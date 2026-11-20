# 16. Single-host deployment behind Cloudflare

- Status: accepted
- Date: 2026-11-21

## Context

The application has to run somewhere public, at no or very low cost, and it constrains where:

- **Always on.** The API syncs platforms every few minutes and pushes new items to phones. Free platform-as-a-service
  plans that stop an idle service (Render, Koyeb) stop the sync with it.
- **Several services.** The API needs PostgreSQL, Redis and an SMTP server; the web image needs the API on the same
  origin (refresh cookie).
- **Mail in.** Forwarded platform e-mails (ADR 0012) arrive at workspace addresses on an inbound domain, so something
  must accept mail for that domain and post it to `POST /api/v1/inbound-mail`.
- **HTTPS with a stable name.** The refresh cookie is `Secure`, Android refuses cleartext by default, and platform
  OAuth apps register a fixed HTTPS redirect URL.

The operator already has a domain whose DNS is on Cloudflare and serves a website at the apex.

## Decision

- **One host, Docker Compose.** `infra/compose.prod.yaml` runs PostgreSQL, Redis, the API, the web image, a backup job
  and a Cloudflare Tunnel connector on one virtual machine. The first host is an Oracle Cloud Always Free Ampere (arm64)
  instance; every image used has an arm64 variant, and the stack does not depend on the provider, so moving to another
  VM means copying the volumes and `.env` files.
- **Cloudflare Tunnel instead of open ports.** `cloudflared` keeps an outbound connection to Cloudflare, which
  terminates TLS and forwards the app's hostname to the web container. The host accepts no inbound HTTP traffic and
  manages no certificates, and its address stays private.
- **The app on a subdomain.** The web app and the API share one subdomain of the operator's domain; the website at
  the apex is untouched. The domain lives only in configuration, never in the repository.
- **Cloudflare Email Routing for mail in, on the apex.** Inbox addresses are random per workspace, so they need a
  catch-all rule, which Email Routing offers only for the apex domain. The apex receives no other mail; personal
  addresses added later get literal rules, which match before the catch-all. The catch-all hands every message to an
  Email Worker (`infra/cloudflare/inbound-mail-worker`) that posts it, unchanged, to the webhook with the shared
  secret. The API keeps doing all parsing; the Worker only relays.
- **An SMTP relay for mail out**, with SPF and DKIM on a sending subdomain, so sign-up and reset e-mails reach the
  inbox and the reputation of the operator's own mail is not at stake. The relay is configured through
  `SPRING_MAIL_*`; which provider is not part of this decision.
- **Daily database dumps on the host**, kept for 14 days. They cover mistakes and failed migrations, not the loss of
  the host; an off-site copy is planned.

## Consequences

- The running cost is the domain the operator already pays for; the free host can be reclaimed or resized by its
  provider, which the provider-neutral stack makes a migration rather than a rewrite.
- Cloudflare sits in front of every request. Client addresses reach the API through `X-Forwarded-For`, which the
  edge appends to and the internal proxies pass on, so per-IP rate limits keep working.
- A single host has no redundancy: an outage or an update of the host stops sync and push until it is back. Mail sent
  meanwhile fails in the Worker; the user still has it in their own mailbox and can forward it again.
- Email Routing rejects mail that fails DMARC. Forwarded platform mail passes on the platform's DKIM signature, which
  mailbox redirect and forwarding rules keep intact.
- Images are built on the host from a checkout, as no registry is used yet.
