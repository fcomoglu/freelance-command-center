package com.freelancecommandcenter.job.api;

import com.freelancecommandcenter.shared.id.JobId;
import com.freelancecommandcenter.shared.platform.PlatformCode;
import java.net.URI;
import java.time.Instant;
import java.util.List;

/**
 * Published once per job, when it enters the catalog. Carries what filters need to decide on it and what a notification
 * shows.
 */
public record JobPublished(
        JobId jobId,
        PlatformCode platform,
        String title,
        String description,
        List<String> skills,
        URI url,
        Instant postedAt) {}
