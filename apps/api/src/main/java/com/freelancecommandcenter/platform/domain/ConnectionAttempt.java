package com.freelancecommandcenter.platform.domain;

import com.freelancecommandcenter.shared.id.WorkspaceId;
import com.freelancecommandcenter.shared.platform.PlatformCode;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Objects;

/**
 * A started OAuth authorization. Its random {@code state} travels through the platform's consent page and back, and
 * binds the returned code to the workspace that started it (protection against login CSRF).
 */
public record ConnectionAttempt(String state, WorkspaceId workspaceId, PlatformCode platform, Instant startedAt) {

    /** Long enough to sign in to the platform and grant access, short enough that a leaked state is soon useless. */
    public static final Duration LIFETIME = Duration.ofMinutes(15);

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int STATE_BYTES = 32;

    public ConnectionAttempt {
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(workspaceId, "workspaceId");
        Objects.requireNonNull(platform, "platform");
        Objects.requireNonNull(startedAt, "startedAt");
    }

    public static ConnectionAttempt start(WorkspaceId workspaceId, PlatformCode platform, Instant now) {
        byte[] bytes = new byte[STATE_BYTES];
        RANDOM.nextBytes(bytes);
        String state = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        return new ConnectionAttempt(state, workspaceId, platform, now);
    }

    /** Whether a code returned with this state may complete a connection of {@code workspaceId} to {@code platform}. */
    public boolean completes(WorkspaceId workspaceId, PlatformCode platform, Instant now) {
        return this.workspaceId.equals(workspaceId)
                && this.platform == platform
                && now.isBefore(startedAt.plus(LIFETIME));
    }
}
