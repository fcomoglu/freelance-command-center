package com.freelancecommandcenter.identity.adapter.out.persistence;

import com.freelancecommandcenter.identity.domain.TokenPurpose;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

interface OneTimeTokenJpaRepository extends JpaRepository<OneTimeTokenEntity, UUID> {

    Optional<OneTimeTokenEntity> findByTokenHash(String tokenHash);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update OneTimeTokenEntity t set t.usedAt = :now"
            + " where t.userId = :userId and t.purpose = :purpose and t.usedAt is null")
    void invalidateOpen(UUID userId, TokenPurpose purpose, Instant now);
}
