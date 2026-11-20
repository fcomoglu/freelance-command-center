package com.freelancecommandcenter.job.api;

import com.freelancecommandcenter.shared.id.JobId;
import com.freelancecommandcenter.shared.platform.PlatformCode;
import java.time.Instant;
import java.util.List;

/** Published once per job, when it enters the catalog. Carries what filters need to decide on it. */
public record JobPublished(
        JobId jobId, PlatformCode platform, String title, String description, List<String> skills, Instant postedAt) {}
