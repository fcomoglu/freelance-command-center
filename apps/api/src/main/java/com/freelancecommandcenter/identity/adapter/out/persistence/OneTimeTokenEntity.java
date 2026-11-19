package com.freelancecommandcenter.identity.adapter.out.persistence;

import com.freelancecommandcenter.identity.domain.OneTimeToken;
import com.freelancecommandcenter.identity.domain.TokenPurpose;
import com.freelancecommandcenter.shared.id.UserId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(schema = "identity", name = "one_time_tokens")
class OneTimeTokenEntity {

    @Id
    private UUID id;

    private UUID userId;

    @Enumerated(EnumType.STRING)
    private TokenPurpose purpose;

    private String tokenHash;
    private Instant createdAt;
    private Instant expiresAt;
    private Instant usedAt;

    protected OneTimeTokenEntity() {}

    OneTimeTokenEntity(UUID id) {
        this.id = id;
    }

    void apply(OneTimeToken token) {
        userId = token.userId().value();
        purpose = token.purpose();
        tokenHash = token.tokenHash();
        createdAt = token.createdAt();
        expiresAt = token.expiresAt();
        usedAt = token.usedAt().orElse(null);
    }

    OneTimeToken toDomain() {
        return OneTimeToken.restore(id, new UserId(userId), purpose, tokenHash, createdAt, expiresAt, usedAt);
    }
}
