-- The platform module owns the platform schema; other modules never read these tables directly.
CREATE SCHEMA platform;

-- High-watermark of the job sync per platform.
CREATE TABLE platform.job_sync_states (
    platform       text PRIMARY KEY,
    watermark      timestamptz,
    last_synced_at timestamptz
);

-- Latest original payload per listing, so a corrected translator can be re-applied without fetching again.
CREATE TABLE platform.raw_job_listings (
    platform    text        NOT NULL,
    external_id text        NOT NULL,
    payload     jsonb       NOT NULL,
    fetched_at  timestamptz NOT NULL,
    PRIMARY KEY (platform, external_id)
);
