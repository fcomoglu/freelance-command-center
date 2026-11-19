-- The identity module owns the identity schema; other modules never read these tables directly.
CREATE SCHEMA identity;

CREATE TABLE identity.users (
    id                uuid PRIMARY KEY,
    email             text        NOT NULL UNIQUE,
    password_hash     text        NOT NULL,
    language          text        NOT NULL,
    email_verified_at timestamptz,
    created_at        timestamptz NOT NULL,
    version           bigint      NOT NULL
);

CREATE TABLE identity.workspaces (
    id            uuid PRIMARY KEY,
    owner_user_id uuid        NOT NULL UNIQUE REFERENCES identity.users (id),
    created_at    timestamptz NOT NULL
);

CREATE TABLE identity.refresh_tokens (
    id           uuid PRIMARY KEY,
    family_id    uuid        NOT NULL,
    user_id      uuid        NOT NULL REFERENCES identity.users (id),
    workspace_id uuid        NOT NULL REFERENCES identity.workspaces (id),
    token_hash   text        NOT NULL UNIQUE,
    created_at   timestamptz NOT NULL,
    expires_at   timestamptz NOT NULL,
    used_at      timestamptz,
    revoked_at   timestamptz
);

CREATE INDEX refresh_tokens_family_idx ON identity.refresh_tokens (family_id);
CREATE INDEX refresh_tokens_user_idx ON identity.refresh_tokens (user_id);

CREATE TABLE identity.one_time_tokens (
    id         uuid PRIMARY KEY,
    user_id    uuid        NOT NULL REFERENCES identity.users (id),
    purpose    text        NOT NULL,
    token_hash text        NOT NULL UNIQUE,
    created_at timestamptz NOT NULL,
    expires_at timestamptz NOT NULL,
    used_at    timestamptz
);

CREATE INDEX one_time_tokens_user_purpose_idx ON identity.one_time_tokens (user_id, purpose);
