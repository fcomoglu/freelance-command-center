package com.freelancecommandcenter.identity.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.freelancecommandcenter.shared.id.UserId;
import com.freelancecommandcenter.shared.id.WorkspaceId;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class RefreshTokenTest {

    private static final Duration LIFETIME = Duration.ofDays(30);
    private static final Instant NOW = Instant.parse("2026-11-19T12:00:00Z");

    private final RefreshToken token =
            RefreshToken.startFamily(UserId.newId(), WorkspaceId.newId(), "hash-1", NOW, LIFETIME);

    @Test
    void rotationUsesTheTokenAndKeepsTheFamily() {
        RefreshToken next = token.rotate("hash-2", NOW.plusSeconds(60), LIFETIME);

        assertThat(token.isReplay()).isTrue();
        assertThat(next.familyId()).isEqualTo(token.familyId());
        assertThat(next.userId()).isEqualTo(token.userId());
        assertThat(next.expiresAt()).isEqualTo(NOW.plusSeconds(60).plus(LIFETIME));
        assertThat(next.isUsable(NOW.plusSeconds(60))).isTrue();
    }

    @Test
    void aRotatedTokenCannotBeRotatedAgain() {
        token.rotate("hash-2", NOW, LIFETIME);

        assertThatThrownBy(() -> token.rotate("hash-3", NOW, LIFETIME)).isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void anExpiredTokenCannotBeRotatedButIsNotAReplay() {
        Instant afterExpiry = NOW.plus(LIFETIME);

        assertThat(token.isReplay()).isFalse();
        assertThatThrownBy(() -> token.rotate("hash-2", afterExpiry, LIFETIME))
                .isInstanceOf(InvalidTokenException.class);
    }
}
