package com.freelancecommandcenter.job.adapter.out.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface JobJpaRepository extends JpaRepository<JobEntity, UUID> {

    Optional<JobEntity> findByPlatformAndExternalId(String platform, String externalId);

    @Query("SELECT j FROM JobEntity j ORDER BY j.postedAt DESC, j.id DESC")
    List<JobEntity> findFeed(Limit limit);

    @Query("""
            SELECT j FROM JobEntity j
            WHERE j.postedAt < :at OR (j.postedAt = :at AND j.id < :id)
            ORDER BY j.postedAt DESC, j.id DESC""")
    List<JobEntity> findFeedAfter(Instant at, UUID id, Limit limit);

    @Query("SELECT j FROM JobEntity j WHERE j.platform = :platform ORDER BY j.postedAt DESC, j.id DESC")
    List<JobEntity> findPlatformFeed(String platform, Limit limit);

    @Query("""
            SELECT j FROM JobEntity j
            WHERE j.platform = :platform AND (j.postedAt < :at OR (j.postedAt = :at AND j.id < :id))
            ORDER BY j.postedAt DESC, j.id DESC""")
    List<JobEntity> findPlatformFeedAfter(String platform, Instant at, UUID id, Limit limit);
}
