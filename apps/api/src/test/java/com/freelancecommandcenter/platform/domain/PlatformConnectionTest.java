package com.freelancecommandcenter.platform.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.freelancecommandcenter.shared.id.WorkspaceId;
import com.freelancecommandcenter.shared.platform.PlatformCode;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PlatformConnectionTest {

    private static final Instant NOW = Instant.parse("2026-11-20T10:00:00Z");
    private static final WorkspaceId WORKSPACE = new WorkspaceId(UUID.randomUUID());
    private static final OAuthTokens TOKENS = new OAuthTokens("access", "refresh", NOW.plus(Duration.ofDays(30)));

    private final PlatformConnection connection = PlatformConnection.connect(
            WORKSPACE, PlatformCode.FREELANCER, new ConnectedAccount("1001", "Ada", TOKENS), NOW);

    @Test
    void aNewConnectionIsActiveAndReadsRecentMessagesFirst() {
        assertThat(connection.status()).isEqualTo(PlatformConnection.Status.ACTIVE);
        assertThat(connection.messagesFrom(Duration.ofDays(7))).isEqualTo(NOW.minus(Duration.ofDays(7)));
    }

    @Test
    void theReadPositionOnlyMovesForward() {
        Instant newest = NOW.plusSeconds(60);

        connection.recordMessagesRead(Optional.of(newest));
        connection.recordMessagesRead(Optional.of(NOW));
        connection.recordMessagesRead(Optional.empty());

        assertThat(connection.messagesFrom(Duration.ofDays(7))).isEqualTo(newest);
    }

    @Test
    void tokensAreLeftOutOfLogs() {
        assertThat(TOKENS.toString()).doesNotContain("access").doesNotContain("refresh");
    }

    @Test
    void tokensAreRenewedAheadOfExpiry() {
        assertThat(TOKENS.expiresWithin(Duration.ofDays(1), NOW)).isFalse();
        assertThat(TOKENS.expiresWithin(Duration.ofDays(1), NOW.plus(Duration.ofDays(29))))
                .isTrue();
    }

    @Test
    void anAttemptCompletesOnlyItsOwnWorkspaceAndPlatformWithinItsLifetime() {
        ConnectionAttempt attempt = ConnectionAttempt.start(WORKSPACE, PlatformCode.FREELANCER, NOW);
        WorkspaceId other = new WorkspaceId(UUID.randomUUID());

        assertThat(attempt.state()).hasSizeGreaterThanOrEqualTo(43);
        assertThat(attempt.completes(WORKSPACE, PlatformCode.FREELANCER, NOW.plusSeconds(60)))
                .isTrue();
        assertThat(attempt.completes(other, PlatformCode.FREELANCER, NOW)).isFalse();
        assertThat(attempt.completes(WORKSPACE, PlatformCode.UPWORK, NOW)).isFalse();
        assertThat(attempt.completes(WORKSPACE, PlatformCode.FREELANCER, NOW.plus(ConnectionAttempt.LIFETIME)))
                .isFalse();
    }
}
