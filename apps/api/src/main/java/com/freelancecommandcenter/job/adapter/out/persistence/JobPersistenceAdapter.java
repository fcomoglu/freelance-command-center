package com.freelancecommandcenter.job.adapter.out.persistence;

import com.freelancecommandcenter.job.application.port.out.JobRepository;
import com.freelancecommandcenter.job.domain.Job;
import com.freelancecommandcenter.shared.id.WorkspaceId;
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
    public Optional<Job> findByListing(Optional<WorkspaceId> owner, PlatformCode platform, String externalId) {
        Optional<JobEntity> entity = owner.isPresent()
                ? jobs.findByOwnerWorkspaceIdAndPlatformAndExternalId(
                        owner.get().value(), platform.value(), externalId)
                : jobs.findByOwnerWorkspaceIdIsNullAndPlatformAndExternalId(platform.value(), externalId);
        return entity.map(JobEntity::toDomain);
    }

    @Override
    public void save(Job job) {
        UUID id = job.id().value();
        JobEntity entity = jobs.findById(id).orElseGet(() -> new JobEntity(id));
        entity.apply(job);
        jobs.save(entity);
    }

    /**
     * One query per filter combination keeps every variant on its index (see V4__job.sql); the owner condition filters
     * while the index is read in feed order.
     */
    @Override
    public List<Job> findFeed(
            WorkspaceId viewer, Optional<PlatformCode> platform, Optional<KeysetCursor> after, int limit) {
        UUID workspace = viewer.value();
        Limit max = Limit.of(limit);
        List<JobEntity> rows;
        if (platform.isPresent()) {
            String code = platform.get().value();
            rows = after.map(cursor -> jobs.findPlatformFeedAfter(workspace, code, cursor.at(), cursor.id(), max))
                    .orElseGet(() -> jobs.findPlatformFeed(workspace, code, max));
        } else {
            rows = after.map(cursor -> jobs.findFeedAfter(workspace, cursor.at(), cursor.id(), max))
                    .orElseGet(() -> jobs.findFeed(workspace, max));
        }
        return rows.stream().map(JobEntity::toDomain).toList();
    }
}
