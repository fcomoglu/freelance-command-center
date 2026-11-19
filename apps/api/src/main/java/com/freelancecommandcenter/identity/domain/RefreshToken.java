package com.freelancecommandcenter.identity.domain;

import com.freelancecommandcenter.shared.id.UserId;
import com.freelancecommandcenter.shared.id.Uuids;
import com.freelancecommandcenter.shared.id.WorkspaceId;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * One link in a refresh token family (one family per signed-in device). Every use rotates the token; presenting a
 * token that was already rotated or revoked is a replay and must revoke the whole family.
 */
public final class RefreshToken {

    private final UUID id;
    private final UUID familyId;
    private final UserId userId;
    private final WorkspaceId workspaceId;
    private final String tokenHash;
    private final Instant createdAt;
    private final Instant expiresAt;
    private Instant usedAt;
    private Instant revokedAt;

    private RefreshToken(
            UUID id,
            UUID familyId,
            UserId userId,
            WorkspaceId workspaceId,
            String tokenHash,
            Instant createdAt,
            Instant expiresAt,
            Instant usedAt,
            Instant revokedAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.familyId = Objects.requireNonNull(familyId, "familyId");
        this.userId = Objects.requireNonNull(userId, "userId");
        this.workspaceId = Objects.requireNonNull(workspaceId, "workspaceId");
        this.tokenHash = Objects.requireNonNull(tokenHash, "tokenHash");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt");
        this.usedAt = usedAt;
        this.revokedAt = revokedAt;
    }

    public static RefreshToken startFamily(
            UserId userId, WorkspaceId workspaceId, String tokenHash, Instant now, Duration lifetime) {
        return new RefreshToken(
                Uuids.v7(), Uuids.v7(), userId, workspaceId, tokenHash, now, now.plus(lifetime), null, null);
    }

    /** Rebuilds a token from storage. */
    public static RefreshToken restore(
            UUID id,
            UUID familyId,
            UserId userId,
            WorkspaceId workspaceId,
            String tokenHash,
            Instant createdAt,
            Instant expiresAt,
            Instant usedAt,
            Instant revokedAt) {
        return new RefreshToken(id, familyId, userId, workspaceId, tokenHash, createdAt, expiresAt, usedAt, revokedAt);
    }

    /** True when the token was already rotated or revoked, i.e. someone is replaying an old token. */
    public boolean isReplay() {
        return usedAt != null || revokedAt != null;
    }

    public boolean isUsable(Instant now) {
        return !isReplay() && now.isBefore(expiresAt);
    }

    /** Marks this token as used and returns its successor in the same family. */
    public RefreshToken rotate(String newTokenHash, Instant now, Duration lifetime) {
        if (!isUsable(now)) {
            throw new InvalidTokenException();
        }
        usedAt = now;
        return new RefreshToken(
                Uuids.v7(), familyId, userId, workspaceId, newTokenHash, now, now.plus(lifetime), null, null);
    }

    public UUID id() {
        return id;
    }

    public UUID familyId() {
        return familyId;
    }

    public UserId userId() {
        return userId;
    }

    public WorkspaceId workspaceId() {
        return workspaceId;
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

    public Optional<Instant> revokedAt() {
        return Optional.ofNullable(revokedAt);
    }
}
