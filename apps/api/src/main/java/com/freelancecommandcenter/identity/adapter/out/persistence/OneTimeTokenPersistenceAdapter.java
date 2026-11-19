package com.freelancecommandcenter.identity.adapter.out.persistence;

import com.freelancecommandcenter.identity.application.port.out.OneTimeTokenRepository;
import com.freelancecommandcenter.identity.domain.OneTimeToken;
import com.freelancecommandcenter.identity.domain.TokenPurpose;
import com.freelancecommandcenter.shared.id.UserId;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
class OneTimeTokenPersistenceAdapter implements OneTimeTokenRepository {

    private final OneTimeTokenJpaRepository tokens;

    OneTimeTokenPersistenceAdapter(OneTimeTokenJpaRepository tokens) {
        this.tokens = tokens;
    }

    @Override
    public Optional<OneTimeToken> findByHash(String tokenHash) {
        return tokens.findByTokenHash(tokenHash).map(OneTimeTokenEntity::toDomain);
    }

    @Override
    public void save(OneTimeToken token) {
        OneTimeTokenEntity entity = tokens.findById(token.id()).orElseGet(() -> new OneTimeTokenEntity(token.id()));
        entity.apply(token);
        tokens.save(entity);
    }

    @Override
    public void invalidateOpen(UserId userId, TokenPurpose purpose, Instant now) {
        tokens.invalidateOpen(userId.value(), purpose, now);
    }
}
