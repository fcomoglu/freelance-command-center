-- Newest OAuth refresh token of the server's own platform credential (one per platform), so token rotation survives
-- restarts.
CREATE TABLE platform.oauth_refresh_tokens (
    platform      text PRIMARY KEY,
    refresh_token text        NOT NULL,
    updated_at    timestamptz NOT NULL
);
