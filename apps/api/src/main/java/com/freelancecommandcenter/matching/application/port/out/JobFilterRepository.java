package com.freelancecommandcenter.matching.application.port.out;

import com.freelancecommandcenter.matching.domain.JobFilter;
import com.freelancecommandcenter.shared.id.WorkspaceId;
import com.freelancecommandcenter.shared.platform.PlatformCode;
import java.util.List;
import java.util.Optional;

public interface JobFilterRepository {

    Optional<JobFilter> find(WorkspaceId workspaceId);

    void save(JobFilter filter);

    /** Filters that include {@code platform} and have at least one keyword, the only ones that can match its jobs. */
    List<JobFilter> findActiveFor(PlatformCode platform);
}
