package com.freelancecommandcenter.matching.api;

import com.freelancecommandcenter.shared.id.JobId;
import com.freelancecommandcenter.shared.id.WorkspaceId;
import com.freelancecommandcenter.shared.platform.PlatformCode;
import java.net.URI;
import java.time.Instant;

/** A new job passed the job filter of a workspace. Listeners must tolerate repeats for the same workspace and job. */
public record JobMatched(
        WorkspaceId workspaceId, JobId jobId, PlatformCode platform, String title, URI url, Instant postedAt) {}
