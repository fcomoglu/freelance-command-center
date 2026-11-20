# 15. One notification feed for proposal decisions and platform notices

- Status: accepted
- Date: 2026-11-21

## Context

Notifications are the third part of the one inbox (ADR 0011), next to jobs and messages. Two kinds matter to a
freelancer:

- **Decisions on their proposals:** shortlisted, awarded, rejected, an award revoked. Freelancer.com reports bids with
  their current status through its projects API, for the account that authorized the request. It reports no history
  of status changes, and only an award carries a time. Reading bids needs the advanced scope `fln:project_manage`,
  which also allows managing bids; Freelancer.com has no read-only scope for them.
- **Everything else the platforms announce:** orders, reviews, payments, profile views, buyer briefs. Fiverr, Bionluk
  and Arc.dev tell them only by e-mail (ADR 0012), each in its own templates, and new templates appear without notice.

Parsers for the e-mail templates are written from real samples, one kind of mail at a time. Until a parser exists, a
forwarded mail was stored as unrecognized and shown nowhere but in the list of forwarded mails.

## Decision

- The `platform` module delivers every notification, from an API or an e-mail, as a `NotificationReceived` event in
  the canonical `PlatformNotification` form: platform, the platform's id, a kind, a title, a plain-text body, a link
  to the platform and a time. The kinds are `proposal_shortlisted`, `proposal_awarded`, `proposal_rejected`,
  `proposal_revoked` (clients translate them) and `notice` (title and body carry the platform's own text).
- The `notification` module keeps **one feed per workspace**, each notification once, with **unread state kept by the
  application** like messages (ADR 0013), and pushes every new notification to the workspace's phones.
- **Proposal decisions** are read with the connected account's tokens in the same task as its messages, so the two
  reads never renew the same tokens at the same time. A change is a status that differs from the one stored at the
  previous read. The first read after connecting only learns the statuses, so connecting does not replay the account's
  history. A pending, withdrawn or closed proposal is not notified. The application only reads bids; the wider scope
  is asked for because no narrower one exists.
- **A platform mail that no parser understands is shown as a notice:** its subject, the start of its text and a link to
  the platform's site. The mail stays `unrecognized`, so it can be read again by a parser written later.

## Consequences

- Every forwarded platform mail reaches the feed from day one, before any parser exists for it; parsers then improve
  what is shown instead of deciding whether anything is shown.
- Mails the user did not want to see (newsletters, marketing) appear as notices too; the user's forwarding rule decides
  what is forwarded.
- A proposal decision is noticed within one sync interval. A decision that is reverted between two reads (shortlisted
  and removed again) is missed, and its time is the time of the read unless the platform reports one.
- Users approve the scope to manage bids when connecting Freelancer.com, although the application never uses it to
  change anything. Accounts connected with the messaging scope only must be connected again.
- Upwork proposal status can join as another `ProposalStatusTracker` without changes outside its adapter.
