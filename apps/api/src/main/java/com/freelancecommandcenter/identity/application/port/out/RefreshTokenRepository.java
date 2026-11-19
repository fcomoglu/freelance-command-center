package com.freelancecommandcenter.identity.application.port.out;

import com.freelancecommandcenter.identity.domain.RefreshToken;
import com.freelancecommandcenter.shared.id.UserId;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository {

    Optional<RefreshToken> findByHash(String tokenHash);

    void save(RefreshToken token);

    void revokeFamily(UUID familyId, Instant now);

    void revokeAllOf(UserId userId, Instant now);
}
