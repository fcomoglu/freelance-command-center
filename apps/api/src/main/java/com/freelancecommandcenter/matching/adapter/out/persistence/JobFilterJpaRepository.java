package com.freelancecommandcenter.matching.adapter.out.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.NativeQuery;

interface JobFilterJpaRepository extends JpaRepository<JobFilterEntity, UUID> {

    /** Written to use the partial GIN index of V6__matching.sql. */
    @NativeQuery("""
            SELECT * FROM matching.job_filters
            WHERE platforms @> ARRAY[CAST(:platform AS text)] AND cardinality(keywords) > 0
            """)
    List<JobFilterEntity> findActiveFor(String platform);
}
