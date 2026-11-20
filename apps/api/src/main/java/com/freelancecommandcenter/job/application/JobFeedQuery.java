package com.freelancecommandcenter.job.application;

import com.freelancecommandcenter.job.application.port.out.JobRepository;
import com.freelancecommandcenter.job.domain.Job;
import com.freelancecommandcenter.shared.id.WorkspaceId;
import com.freelancecommandcenter.shared.pagination.KeysetCursor;
import com.freelancecommandcenter.shared.pagination.Page;
import com.freelancecommandcenter.shared.platform.PlatformCode;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The job feed of a workspace: public jobs and its private ones, newest posting first. */
@Service
public class JobFeedQuery {

    private final JobRepository jobs;

    JobFeedQuery(JobRepository jobs) {
        this.jobs = jobs;
    }

    @Transactional(readOnly = true)
    public Page<Job> list(
            WorkspaceId viewer, Optional<PlatformCode> platform, Optional<KeysetCursor> after, int limit) {
        return Page.of(
                jobs.findFeed(viewer, platform, after, limit + 1),
                limit,
                job -> new KeysetCursor(job.postedAt(), job.id().value()));
    }
}
