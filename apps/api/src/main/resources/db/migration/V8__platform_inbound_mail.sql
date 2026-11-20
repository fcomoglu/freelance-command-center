-- Inbox addresses for forwarded platform notification e-mails (ADR 0012), one per workspace.
CREATE TABLE platform.mail_inboxes (
    workspace_id uuid PRIMARY KEY,
    local_part   text        NOT NULL UNIQUE,
    created_at   timestamptz NOT NULL
);

-- Every mail an inbox received, with the raw message so that improved parsers can read it again.
CREATE TABLE platform.inbound_mails (
    id           uuid PRIMARY KEY,
    workspace_id uuid        NOT NULL,
    message_id   text        NOT NULL,
    platform     text,
    sender       text        NOT NULL,
    subject      text        NOT NULL,
    sent_at      timestamptz,
    received_at  timestamptz NOT NULL,
    status       text        NOT NULL,
    raw          bytea       NOT NULL,
    UNIQUE (workspace_id, message_id)
);

CREATE INDEX inbound_mails_recent_idx ON platform.inbound_mails (workspace_id, received_at DESC, id DESC);
