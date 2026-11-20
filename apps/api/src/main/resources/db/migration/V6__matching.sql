-- The matching module owns the matching schema; other modules never read these tables directly.
CREATE SCHEMA matching;

-- One job filter per workspace; a workspace without a row uses the default filter (all platforms, no keywords).
CREATE TABLE matching.job_filters (
    workspace_id uuid PRIMARY KEY,
    keywords     text[] NOT NULL,
    platforms    text[] NOT NULL
);

-- Every new job looks up the filters of its platform that have keywords.
CREATE INDEX job_filters_platforms_idx ON matching.job_filters USING gin (platforms)
    WHERE cardinality(keywords) > 0;
