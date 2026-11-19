package com.freelancecommandcenter.identity.application.port.out;

import com.freelancecommandcenter.identity.domain.OneTimeToken;
import com.freelancecommandcenter.identity.domain.TokenPurpose;
import com.freelancecommandcenter.shared.id.UserId;
import java.time.Instant;
import java.util.Optional;

public interface OneTimeTokenRepository {

    Optional<OneTimeToken> findByHash(String tokenHash);

    void save(OneTimeToken token);

    /** Marks every unused token of the user for this purpose as used, so only the newest link works. */
    void invalidateOpen(UserId userId, TokenPurpose purpose, Instant now);
}
