package com.freelancecommandcenter.platform.domain;

import com.freelancecommandcenter.shared.id.Uuids;
import com.freelancecommandcenter.shared.id.WorkspaceId;
import com.freelancecommandcenter.shared.platform.PlatformCode;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * A workspace's authorized account on a platform (ADR 0014): the OAuth tokens the application reads the account's
 * messages with, and how far the messages have been read. A workspace has at most one connection per platform.
 */
public final class PlatformConnection {

    public enum Status {
        ACTIVE,
        /** The platform no longer accepts the tokens; only connecting the account again helps. */
        RECONNECT_REQUIRED
    }

    private final UUID id;
    private final WorkspaceId workspaceId;
    private final PlatformCode platform;
    private final String accountId;
    private final String accountName;
    private final Instant connectedAt;
    private OAuthTokens tokens;
    private Status status;
    private Instant messagesReadUntil;

    private PlatformConnection(
            UUID id,
            WorkspaceId workspaceId,
            PlatformCode platform,
            String accountId,
            String accountName,
            Instant connectedAt,
            OAuthTokens tokens,
            Status status,
            Instant messagesReadUntil) {
        this.id = Objects.requireNonNull(id, "id");
        this.workspaceId = Objects.requireNonNull(workspaceId, "workspaceId");
        this.platform = Objects.requireNonNull(platform, "platform");
        this.accountId = Objects.requireNonNull(accountId, "accountId");
        this.accountName = Objects.requireNonNull(accountName, "accountName");
        this.connectedAt = Objects.requireNonNull(connectedAt, "connectedAt");
        this.tokens = Objects.requireNonNull(tokens, "tokens");
        this.status = Objects.requireNonNull(status, "status");
        this.messagesReadUntil = messagesReadUntil;
    }

    public static PlatformConnection connect(
            WorkspaceId workspaceId, PlatformCode platform, ConnectedAccount account, Instant now) {
        return new PlatformConnection(
                Uuids.v7(),
                workspaceId,
                platform,
                account.accountId(),
                account.name(),
                now,
                account.tokens(),
                Status.ACTIVE,
                null);
    }

    /** Rebuilds a stored connection. */
    public static PlatformConnection restore(
            UUID id,
            WorkspaceId workspaceId,
            PlatformCode platform,
            String accountId,
            String accountName,
            Instant connectedAt,
            OAuthTokens tokens,
            Status status,
            Instant messagesReadUntil) {
        return new PlatformConnection(
                id, workspaceId, platform, accountId, accountName, connectedAt, tokens, status, messagesReadUntil);
    }

    public void renewTokens(OAuthTokens renewed) {
        this.tokens = Objects.requireNonNull(renewed, "renewed");
    }

    public void requireReconnect() {
        this.status = Status.RECONNECT_REQUIRED;
    }

    /**
     * Where the next message read starts: at the newest message read so far, inclusive, so messages sent in the same
     * second are not missed (re-reading one is harmless). A new connection starts {@code initialLookback} before it was
     * made, so the inbox shows recent conversations right away.
     */
    public Instant messagesFrom(Duration initialLookback) {
        return messagesReadUntil != null ? messagesReadUntil : connectedAt.minus(initialLookback);
    }

    /** Moves the read position forward to the newest message read; never backwards. */
    public void recordMessagesRead(Optional<Instant> newest) {
        newest.filter(sentAt -> messagesReadUntil == null || sentAt.isAfter(messagesReadUntil))
                .ifPresent(sentAt -> messagesReadUntil = sentAt);
    }

    public UUID id() {
        return id;
    }

    public WorkspaceId workspaceId() {
        return workspaceId;
    }

    public PlatformCode platform() {
        return platform;
    }

    public String accountId() {
        return accountId;
    }

    public String accountName() {
        return accountName;
    }

    public Instant connectedAt() {
        return connectedAt;
    }

    public OAuthTokens tokens() {
        return tokens;
    }

    public Status status() {
        return status;
    }

    public Optional<Instant> messagesReadUntil() {
        return Optional.ofNullable(messagesReadUntil);
    }
}
