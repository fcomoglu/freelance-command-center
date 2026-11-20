package com.freelancecommandcenter.matching.application;

import com.freelancecommandcenter.job.api.JobPublished;
import com.freelancecommandcenter.matching.api.JobMatched;
import com.freelancecommandcenter.matching.application.port.out.JobFilterRepository;
import com.freelancecommandcenter.matching.domain.JobFilter;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Checks a new job against every active job filter of its platform. Filters are evaluated in memory, which is cheap
 * for a keyword list per workspace; the events go out with the outbox in the same transaction.
 */
@Service
public class MatchJobUseCase {

    private final JobFilterRepository filters;
    private final ApplicationEventPublisher events;

    MatchJobUseCase(JobFilterRepository filters, ApplicationEventPublisher events) {
        this.filters = filters;
        this.events = events;
    }

    /** Returns the number of workspaces the job matched. */
    @Transactional
    public int match(JobPublished job) {
        int matched = 0;
        for (JobFilter filter : filters.findActiveFor(job.platform())) {
            if (filter.matches(job.platform(), job.title(), job.description(), job.skills())) {
                events.publishEvent(new JobMatched(
                        filter.workspaceId(), job.jobId(), job.platform(), job.title(), job.url(), job.postedAt()));
                matched++;
            }
        }
        return matched;
    }
}
