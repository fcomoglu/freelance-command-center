-- Platform accounts connected by workspaces through OAuth (ADR 0014), at most one per workspace and platform. The
-- tokens are encrypted by the application (AES-256-GCM: nonce followed by ciphertext).
CREATE TABLE platform.connections (
    id                  uuid PRIMARY KEY,
    workspace_id        uuid        NOT NULL,
    platform            text        NOT NULL,
    account_id          text        NOT NULL,
    account_name        text        NOT NULL,
    access_token        bytea       NOT NULL,
    refresh_token       bytea       NOT NULL,
    token_expires_at    timestamptz NOT NULL,
    status              text        NOT NULL,
    connected_at        timestamptz NOT NULL,
    messages_read_until timestamptz,
    UNIQUE (workspace_id, platform)
);

CREATE INDEX connections_platform_status_idx ON platform.connections (platform, status);

-- Started OAuth authorizations; each state completes at most one connection.
CREATE TABLE platform.connection_attempts (
    state        text PRIMARY KEY,
    workspace_id uuid        NOT NULL,
    platform     text        NOT NULL,
    started_at   timestamptz NOT NULL
);
