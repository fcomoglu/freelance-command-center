-- Messages a workspace was notified about; the key makes repeated message events harmless.
CREATE TABLE notification.message_notifications (
    workspace_id uuid        NOT NULL,
    message_id   uuid        NOT NULL,
    notified_at  timestamptz NOT NULL,
    PRIMARY KEY (workspace_id, message_id)
);
