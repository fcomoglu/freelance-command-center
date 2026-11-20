package com.freelancecommandcenter.job.application.port.out;

import com.freelancecommandcenter.job.domain.Job;
import com.freelancecommandcenter.shared.id.WorkspaceId;
import com.freelancecommandcenter.shared.pagination.KeysetCursor;
import com.freelancecommandcenter.shared.platform.PlatformCode;
import java.util.List;
import java.util.Optional;

public interface JobRepository {

    /** @param owner empty for the public job of the listing */
    Optional<Job> findByListing(Optional<WorkspaceId> owner, PlatformCode platform, String externalId);

    void save(Job job);

    /**
     * Jobs the viewer sees (public ones and its own), newest posting first (then by id), after {@code after} when given.
     *
     * @param platform only jobs of this platform when given
     */
    List<Job> findFeed(WorkspaceId viewer, Optional<PlatformCode> platform, Optional<KeysetCursor> after, int limit);
}
