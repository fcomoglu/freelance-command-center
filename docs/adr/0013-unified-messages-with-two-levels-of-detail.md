# 13. Unified messages with two levels of detail; replies stay on the platform

- Status: accepted
- Date: 2026-11-20

## Context

Messages are the second part of the one inbox (ADR 0011). The platforms offer them very differently:

- Freelancer.com and Upwork have official messaging APIs. They return whole conversations, but only for the account
  that authorized the request, so every workspace needs its own OAuth connection; the single server credential of the
  public job sync (ADR 0007) would expose one account's messages to everyone.
- Fiverr, Bionluk and Arc.dev are read only from their notification e-mails (ADR 0012). Such an e-mail says who wrote
  and links to the conversation; whether it carries the message text depends on the platform's template.

Sending a reply is an action on the user's behalf, and the product never submits anything to a platform without an
explicit user action.

## Decision

- The `messaging` module keeps **one inbox of conversations** per workspace, whatever the source. A conversation is
  identified by its platform and the platform's conversation id, and links to the conversation on the platform.
- Every message records its **level of detail**:
  - `full`: read from a platform API; the body is the whole message.
  - `notice`: read from a notification e-mail; the body is what the e-mail shows, possibly a preview or nothing.
  The clients show the difference and send the user to the platform for anything a notice lacks.
- The `platform` module delivers messages from both sources as the same `MessageReceived` event, so `messaging` does
  not know where a message came from beyond its level of detail.
- **Unread state is kept by the application.** It cannot be read from e-mail, and a conversation is read when the
  user opens it in the application.
- Messages are always **private** to their workspace.
- **Replies happen on the platform.** Replying from the application can be added later for the API platforms, as a
  separate capability (`MessageSender`) that only sends on an explicit user action.
- Reading messages through the APIs needs **per-workspace platform connections** (OAuth 2, tokens stored encrypted);
  they are built as the `AccountConnector` capability of ADR 0010.

## Consequences

- Every platform appears in the inbox from the start: the e-mail path works without any platform approval, and the
  API platforms join as their connections are built.
- The inbox is uneven: e-mail platforms give notices, not conversations. The clients must make this visible instead of
  presenting a preview as the whole message.
- Read state can differ from the platform's own: a message read on the platform still counts as unread in the
  application until the conversation is opened there.
- Without replies in the application, answering always means a switch to the platform; the conversation link makes it
  one tap.
