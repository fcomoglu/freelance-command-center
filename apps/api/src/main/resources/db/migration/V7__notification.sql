-- The notification module owns the notification schema; other modules never read these tables directly.
CREATE SCHEMA notification;

-- Phones that receive push notifications, one per Expo push token.
CREATE TABLE notification.devices (
    id            uuid PRIMARY KEY,
    push_token    text        NOT NULL UNIQUE,
    user_id       uuid        NOT NULL,
    workspace_id  uuid        NOT NULL,
    language      text        NOT NULL,
    registered_at timestamptz NOT NULL
);

CREATE INDEX devices_workspace_idx ON notification.devices (workspace_id);

-- Jobs a workspace was notified about; the key makes repeated match events harmless.
CREATE TABLE notification.job_notifications (
    workspace_id uuid        NOT NULL,
    job_id       uuid        NOT NULL,
    notified_at  timestamptz NOT NULL,
    PRIMARY KEY (workspace_id, job_id)
);
