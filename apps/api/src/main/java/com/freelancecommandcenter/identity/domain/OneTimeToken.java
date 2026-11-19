package com.freelancecommandcenter.identity.domain;

import com.freelancecommandcenter.shared.id.UserId;
import com.freelancecommandcenter.shared.id.Uuids;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** A single-use token sent by e-mail (verification, password reset). */
public final class OneTimeToken {

    private final UUID id;
    private final UserId userId;
    private final TokenPurpose purpose;
    private final String tokenHash;
    private final Instant createdAt;
    private final Instant expiresAt;
    private Instant usedAt;

    private OneTimeToken(
            UUID id,
            UserId userId,
            TokenPurpose purpose,
            String tokenHash,
            Instant createdAt,
            Instant expiresAt,
            Instant usedAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.userId = Objects.requireNonNull(userId, "userId");
        this.purpose = Objects.requireNonNull(purpose, "purpose");
        this.tokenHash = Objects.requireNonNull(tokenHash, "tokenHash");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt");
        this.usedAt = usedAt;
    }

    public static OneTimeToken issue(UserId userId, TokenPurpose purpose, String tokenHash, Instant now) {
        return new OneTimeToken(Uuids.v7(), userId, purpose, tokenHash, now, now.plus(purpose.lifetime()), null);
    }

    /** Rebuilds a token from storage. */
    public static OneTimeToken restore(
            UUID id,
            UserId userId,
            TokenPurpose purpose,
            String tokenHash,
            Instant createdAt,
            Instant expiresAt,
            Instant usedAt) {
        return new OneTimeToken(id, userId, purpose, tokenHash, createdAt, expiresAt, usedAt);
    }

    public void consume(Instant now) {
        if (usedAt != null || !now.isBefore(expiresAt)) {
            throw new InvalidTokenException();
        }
        usedAt = now;
    }

    public UUID id() {
        return id;
    }

    public UserId userId() {
        return userId;
    }

    public TokenPurpose purpose() {
        return purpose;
    }

    public String tokenHash() {
        return tokenHash;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant expiresAt() {
        return expiresAt;
    }

    public Optional<Instant> usedAt() {
        return Optional.ofNullable(usedAt);
    }
}
