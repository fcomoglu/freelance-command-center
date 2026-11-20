package com.freelancecommandcenter.job.adapter.out.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/** Feed queries return the public jobs plus the viewer's own private ones. */
interface JobJpaRepository extends JpaRepository<JobEntity, UUID> {

    Optional<JobEntity> findByOwnerWorkspaceIdIsNullAndPlatformAndExternalId(String platform, String externalId);

    Optional<JobEntity> findByOwnerWorkspaceIdAndPlatformAndExternalId(
            UUID ownerWorkspaceId, String platform, String externalId);

    @Query("""
            SELECT j FROM JobEntity j
            WHERE (j.ownerWorkspaceId IS NULL OR j.ownerWorkspaceId = :viewer)
            ORDER BY j.postedAt DESC, j.id DESC""")
    List<JobEntity> findFeed(UUID viewer, Limit limit);

    @Query("""
            SELECT j FROM JobEntity j
            WHERE (j.ownerWorkspaceId IS NULL OR j.ownerWorkspaceId = :viewer)
                AND (j.postedAt < :at OR (j.postedAt = :at AND j.id < :id))
            ORDER BY j.postedAt DESC, j.id DESC""")
    List<JobEntity> findFeedAfter(UUID viewer, Instant at, UUID id, Limit limit);

    @Query("""
            SELECT j FROM JobEntity j
            WHERE (j.ownerWorkspaceId IS NULL OR j.ownerWorkspaceId = :viewer) AND j.platform = :platform
            ORDER BY j.postedAt DESC, j.id DESC""")
    List<JobEntity> findPlatformFeed(UUID viewer, String platform, Limit limit);

    @Query("""
            SELECT j FROM JobEntity j
            WHERE (j.ownerWorkspaceId IS NULL OR j.ownerWorkspaceId = :viewer) AND j.platform = :platform
                AND (j.postedAt < :at OR (j.postedAt = :at AND j.id < :id))
            ORDER BY j.postedAt DESC, j.id DESC""")
    List<JobEntity> findPlatformFeedAfter(UUID viewer, String platform, Instant at, UUID id, Limit limit);
}
