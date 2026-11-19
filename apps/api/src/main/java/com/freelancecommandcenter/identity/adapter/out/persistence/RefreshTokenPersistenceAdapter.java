package com.freelancecommandcenter.identity.adapter.out.persistence;

import com.freelancecommandcenter.identity.application.port.out.RefreshTokenRepository;
import com.freelancecommandcenter.identity.domain.RefreshToken;
import com.freelancecommandcenter.shared.id.UserId;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class RefreshTokenPersistenceAdapter implements RefreshTokenRepository {

    private final RefreshTokenJpaRepository tokens;

    RefreshTokenPersistenceAdapter(RefreshTokenJpaRepository tokens) {
        this.tokens = tokens;
    }

    @Override
    public Optional<RefreshToken> findByHash(String tokenHash) {
        return tokens.findByTokenHash(tokenHash).map(RefreshTokenEntity::toDomain);
    }

    @Override
    public void save(RefreshToken token) {
        RefreshTokenEntity entity = tokens.findById(token.id()).orElseGet(() -> new RefreshTokenEntity(token.id()));
        entity.apply(token);
        tokens.save(entity);
    }

    @Override
    public void revokeFamily(UUID familyId, Instant now) {
        tokens.revokeFamily(familyId, now);
    }

    @Override
    public void revokeAllOf(UserId userId, Instant now) {
        tokens.revokeAllOf(userId.value(), now);
    }
}
