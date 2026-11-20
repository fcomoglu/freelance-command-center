-- The messaging module owns the messaging schema; other modules never read these tables directly.
CREATE SCHEMA messaging;

-- One row per platform conversation of a workspace, with the summary the inbox list shows.
CREATE TABLE messaging.conversations (
    id              uuid PRIMARY KEY,
    workspace_id    uuid        NOT NULL,
    platform        text        NOT NULL,
    external_id     text        NOT NULL,
    counterpart     text        NOT NULL,
    url             text        NOT NULL,
    detail          text        NOT NULL,
    last_message_at timestamptz NOT NULL,
    preview         text        NOT NULL,
    unread_count    integer     NOT NULL CHECK (unread_count >= 0),
    CONSTRAINT conversations_platform_key UNIQUE (workspace_id, platform, external_id)
);

-- Keyset pagination of the inbox.
CREATE INDEX conversations_recent_idx ON messaging.conversations (workspace_id, last_message_at DESC, id DESC);

-- The key makes a repeated delivery of a message harmless.
CREATE TABLE messaging.messages (
    id              uuid PRIMARY KEY,
    conversation_id uuid        NOT NULL REFERENCES messaging.conversations (id),
    external_id     text        NOT NULL,
    direction       text        NOT NULL,
    detail          text        NOT NULL,
    body            text        NOT NULL,
    sent_at         timestamptz NOT NULL,
    CONSTRAINT messages_platform_key UNIQUE (conversation_id, external_id)
);

-- Keyset pagination of a conversation's history.
CREATE INDEX messages_history_idx ON messaging.messages (conversation_id, sent_at DESC, id DESC);
