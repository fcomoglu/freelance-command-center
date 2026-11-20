package com.freelancecommandcenter.job.adapter.out.persistence;

import com.freelancecommandcenter.job.application.port.out.JobRepository;
import com.freelancecommandcenter.job.domain.Job;
import com.freelancecommandcenter.shared.pagination.KeysetCursor;
import com.freelancecommandcenter.shared.platform.PlatformCode;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Component;

/** Stores catalog jobs. Updates load the entity first so optimistic locking stays effective. */
@Component
class JobPersistenceAdapter implements JobRepository {

    private final JobJpaRepository jobs;

    JobPersistenceAdapter(JobJpaRepository jobs) {
        this.jobs = jobs;
    }

    @Override
    public Optional<Job> findByListing(PlatformCode platform, String externalId) {
        return jobs.findByPlatformAndExternalId(platform.value(), externalId).map(JobEntity::toDomain);
    }

    @Override
    public void save(Job job) {
        UUID id = job.id().value();
        JobEntity entity = jobs.findById(id).orElseGet(() -> new JobEntity(id));
        entity.apply(job);
        jobs.save(entity);
    }

    /** One query per filter combination keeps every variant on its index (see V4__job.sql). */
    @Override
    public List<Job> findFeed(Optional<PlatformCode> platform, Optional<KeysetCursor> after, int limit) {
        Limit max = Limit.of(limit);
        List<JobEntity> rows;
        if (platform.isPresent()) {
            String code = platform.get().value();
            rows = after.map(cursor -> jobs.findPlatformFeedAfter(code, cursor.at(), cursor.id(), max))
                    .orElseGet(() -> jobs.findPlatformFeed(code, max));
        } else {
            rows = after.map(cursor -> jobs.findFeedAfter(cursor.at(), cursor.id(), max))
                    .orElseGet(() -> jobs.findFeed(max));
        }
        return rows.stream().map(JobEntity::toDomain).toList();
    }
}
