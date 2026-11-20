-- A job read from a forwarded e-mail belongs to the workspace that forwarded it (ADR 0012); jobs read from platform
-- APIs have no owner and every workspace sees them.
ALTER TABLE job.jobs ADD COLUMN owner_workspace_id uuid;

-- A public listing is catalogued once; a private one once per workspace that receives it.
ALTER TABLE job.jobs DROP CONSTRAINT jobs_listing_key;
ALTER TABLE job.jobs
    ADD CONSTRAINT jobs_listing_key UNIQUE NULLS NOT DISTINCT (owner_workspace_id, platform, external_id);
