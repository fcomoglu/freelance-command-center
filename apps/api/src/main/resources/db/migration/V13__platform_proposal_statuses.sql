-- When the proposals of a connection were last read; empty before the first read, which only learns their statuses.
ALTER TABLE platform.connections ADD COLUMN proposals_read_at timestamptz;

-- The last known status of each proposal of a connection; a different status at the next read is a change. Rows go
-- with their connection.
CREATE TABLE platform.proposal_statuses (
    connection_id uuid        NOT NULL REFERENCES platform.connections (id) ON DELETE CASCADE,
    proposal_id   text        NOT NULL,
    status        text        NOT NULL,
    checked_at    timestamptz NOT NULL,
    PRIMARY KEY (connection_id, proposal_id)
);
