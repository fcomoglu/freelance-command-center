package com.freelancecommandcenter.matching.adapter.out.persistence;

import com.freelancecommandcenter.matching.application.port.out.JobFilterRepository;
import com.freelancecommandcenter.matching.domain.JobFilter;
import com.freelancecommandcenter.shared.id.WorkspaceId;
import com.freelancecommandcenter.shared.platform.PlatformCode;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class JobFilterPersistenceAdapter implements JobFilterRepository {

    private final JobFilterJpaRepository filters;

    JobFilterPersistenceAdapter(JobFilterJpaRepository filters) {
        this.filters = filters;
    }

    @Override
    public Optional<JobFilter> find(WorkspaceId workspaceId) {
        return filters.findById(workspaceId.value()).map(JobFilterEntity::toDomain);
    }

    @Override
    public void save(JobFilter filter) {
        UUID id = filter.workspaceId().value();
        JobFilterEntity entity = filters.findById(id).orElseGet(() -> new JobFilterEntity(id));
        entity.apply(filter);
        filters.save(entity);
    }

    @Override
    public List<JobFilter> findActiveFor(PlatformCode platform) {
        return filters.findActiveFor(platform.value()).stream()
                .map(JobFilterEntity::toDomain)
                .toList();
    }
}
