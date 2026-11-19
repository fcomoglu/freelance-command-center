package com.freelancecommandcenter.identity.adapter.out.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface WorkspaceJpaRepository extends JpaRepository<WorkspaceEntity, UUID> {

    Optional<WorkspaceEntity> findByOwnerUserId(UUID ownerUserId);
}
