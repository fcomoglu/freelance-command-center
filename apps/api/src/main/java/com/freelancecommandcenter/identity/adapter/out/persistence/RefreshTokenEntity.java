package com.freelancecommandcenter.identity.adapter.out.persistence;

import com.freelancecommandcenter.identity.domain.RefreshToken;
import com.freelancecommandcenter.shared.id.UserId;
import com.freelancecommandcenter.shared.id.WorkspaceId;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(schema = "identity", name = "refresh_tokens")
class RefreshTokenEntity {

    @Id
    private UUID id;

    private UUID familyId;
    private UUID userId;
    private UUID workspaceId;
    private String tokenHash;
    private Instant createdAt;
    private Instant expiresAt;
    private Instant usedAt;
    private Instant revokedAt;

    protected RefreshTokenEntity() {}

    RefreshTokenEntity(UUID id) {
        this.id = id;
    }

    void apply(RefreshToken token) {
        familyId = token.familyId();
        userId = token.userId().value();
        workspaceId = token.workspaceId().value();
        tokenHash = token.tokenHash();
        createdAt = token.createdAt();
        expiresAt = token.expiresAt();
        usedAt = token.usedAt().orElse(null);
        revokedAt = token.revokedAt().orElse(null);
    }

    RefreshToken toDomain() {
        return RefreshToken.restore(
                id,
                familyId,
                new UserId(userId),
                new WorkspaceId(workspaceId),
                tokenHash,
                createdAt,
                expiresAt,
                usedAt,
                revokedAt);
    }
}
