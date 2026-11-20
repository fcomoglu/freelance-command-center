-- The job module owns the job schema; other modules never read these tables directly.
CREATE SCHEMA job;

CREATE TABLE job.jobs (
    id                uuid PRIMARY KEY,
    platform          text        NOT NULL,
    external_id       text        NOT NULL,
    title             text        NOT NULL,
    description       text        NOT NULL,
    url               text        NOT NULL,
    budget_type       text,
    budget_currency   text,
    budget_minimum    numeric(19, 4),
    budget_maximum    numeric(19, 4),
    skills            text[]      NOT NULL,
    bid_count         integer,
    language          text,
    posted_at         timestamptz NOT NULL,
    source_updated_at timestamptz NOT NULL,
    discovered_at     timestamptz NOT NULL,
    version           bigint      NOT NULL,
    CONSTRAINT jobs_listing_key UNIQUE (platform, external_id)
);

-- Keyset pagination of the feed, with and without a platform filter.
CREATE INDEX jobs_feed_idx ON job.jobs (posted_at DESC, id DESC);
CREATE INDEX jobs_platform_feed_idx ON job.jobs (platform, posted_at DESC, id DESC);
