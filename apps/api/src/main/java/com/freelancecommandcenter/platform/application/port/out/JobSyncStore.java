package com.freelancecommandcenter.platform.application.port.out;

import com.freelancecommandcenter.platform.application.port.out.JobSource.FetchedListing;
import com.freelancecommandcenter.platform.domain.JobSyncState;
import com.freelancecommandcenter.shared.platform.PlatformCode;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** Storage of the job sync: the per-platform watermark and the raw payload of every fetched listing. */
public interface JobSyncStore {

    Optional<JobSyncState> findState(PlatformCode platform);

    void saveState(JobSyncState state);

    /** Keeps the latest raw payload per listing. */
    void saveRawListings(List<FetchedListing> listings, Instant fetchedAt);
}
