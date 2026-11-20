# 11. Product focus: one inbox for jobs, messages and notifications

- Status: accepted
- Date: 2026-11-20

## Context

The first product description centered on AI scoring and proposal drafting, fed by a professional profile that the
freelancer would fill in inside the product. In practice every platform already has its own account and profile, and
re-entering them adds work without adding value. The daily cost for a freelancer working on several platforms is
elsewhere: checking each site for new jobs, unanswered messages and bid status changes.

## Decision

- The core of the product is **one inbox** for all connected platforms (Freelancer.com, Upwork, Fiverr, Bionluk,
  Arc.dev): a job feed, conversations and platform notifications, with push notifications on the phone.
- Order of delivery: the **job feed** with new-job notifications first (v0.0.1, all five platforms), **messages**
  next, **AI scoring and proposal drafts** after that.
- The product has no hand-edited professional profile. When AI scoring is built, the profile it needs is imported
  from the connected platform accounts.
- Product accounts stay: they own the platform connections and the stored jobs, messages and notifications.
- Replies and bids are sent on the platform, or by the product only after an explicit user action
  ([ADR 0004](0004-platform-integration-via-api-or-user-driven-extension.md)).

## Consequences

- A `messaging` module holds conversations and messages per workspace; `notification` becomes a core module rather
  than a side channel.
- The platform layer gains capability ports for messages and notifications next to `JobSource`.
- The browser extension is needed in the first release, because three of the five platforms have no public API.
- Upwork requires an approved API key; its integration depends on the approval date.
- The `profile` module changes from an edited profile to an imported, read-only one.
