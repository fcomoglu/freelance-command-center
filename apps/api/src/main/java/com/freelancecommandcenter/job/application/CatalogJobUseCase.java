package com.freelancecommandcenter.job.application;

import com.freelancecommandcenter.job.api.JobPublished;
import com.freelancecommandcenter.job.application.port.out.JobRepository;
import com.freelancecommandcenter.job.domain.Job;
import com.freelancecommandcenter.job.domain.JobBudget;
import com.freelancecommandcenter.job.domain.JobContent;
import com.freelancecommandcenter.platform.api.JobListing;
import java.time.Clock;
import java.util.Optional;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Adds a discovered listing to the catalog or updates the job it already became. */
@Service
public class CatalogJobUseCase {

    private final JobRepository jobs;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    CatalogJobUseCase(JobRepository jobs, ApplicationEventPublisher events, Clock clock) {
        this.jobs = jobs;
        this.events = events;
        this.clock = clock;
    }

    /** Idempotent: the same listing again, or an older version of it, leaves the catalog unchanged. */
    @Transactional
    public void record(JobListing listing) {
        JobContent content = content(listing);
        Optional<Job> existing = jobs.findByListing(listing.platform(), listing.externalId());
        if (existing.isPresent()) {
            Job job = existing.get();
            if (job.update(content, listing.updatedAt())) {
                jobs.save(job);
            }
            return;
        }
        Job job = Job.discover(
                listing.platform(),
                listing.externalId(),
                content,
                listing.postedAt(),
                listing.updatedAt(),
                clock.instant());
        jobs.save(job);
        events.publishEvent(new JobPublished(
                job.id(), job.platform(), content.title(), content.description(), content.skills(), job.postedAt()));
    }

    private static JobContent content(JobListing listing) {
        return new JobContent(
                listing.title(),
                listing.description(),
                listing.url(),
                Optional.ofNullable(listing.budget()).map(CatalogJobUseCase::budget),
                listing.skills(),
                Optional.ofNullable(listing.bidCount()),
                Optional.ofNullable(listing.language()));
    }

    private static JobBudget budget(JobListing.Budget budget) {
        JobBudget.Type type = switch (budget.type()) {
            case FIXED -> JobBudget.Type.FIXED;
            case HOURLY -> JobBudget.Type.HOURLY;
        };
        return new JobBudget(type, budget.minimum(), Optional.ofNullable(budget.maximum()));
    }
}
