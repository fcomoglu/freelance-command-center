-- The notification feed of each workspace (ADR 0015); the key makes a repeated delivery of a platform notification
-- harmless.
CREATE TABLE notification.notifications (
    id           uuid PRIMARY KEY,
    workspace_id uuid        NOT NULL,
    platform     text        NOT NULL,
    external_id  text        NOT NULL,
    kind         text        NOT NULL,
    title        text        NOT NULL,
    body         text        NOT NULL,
    url          text        NOT NULL,
    occurred_at  timestamptz NOT NULL,
    read_at      timestamptz,
    CONSTRAINT notifications_platform_key UNIQUE (workspace_id, platform, external_id)
);

-- Keyset pagination of the feed.
CREATE INDEX notifications_recent_idx ON notification.notifications (workspace_id, occurred_at DESC, id DESC);
