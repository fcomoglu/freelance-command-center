package com.freelancecommandcenter.identity.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.freelancecommandcenter.shared.id.UserId;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class OneTimeTokenTest {

    private static final Instant NOW = Instant.parse("2026-11-19T12:00:00Z");

    @Test
    void canBeConsumedOnceBeforeItExpires() {
        OneTimeToken token = OneTimeToken.issue(UserId.newId(), TokenPurpose.PASSWORD_RESET, "hash", NOW);

        token.consume(NOW.plusSeconds(60));

        assertThat(token.usedAt()).contains(NOW.plusSeconds(60));
        assertThatThrownBy(() -> token.consume(NOW.plusSeconds(61))).isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void cannotBeConsumedAfterItsPurposeLifetime() {
        OneTimeToken token = OneTimeToken.issue(UserId.newId(), TokenPurpose.PASSWORD_RESET, "hash", NOW);

        assertThatThrownBy(() -> token.consume(NOW.plus(TokenPurpose.PASSWORD_RESET.lifetime())))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void opaqueTokensAreRandomAndOnlyTheirHashIsStable() {
        OpaqueToken first = OpaqueToken.generate();
        OpaqueToken second = OpaqueToken.generate();

        assertThat(first.value()).isNotEqualTo(second.value()).hasSize(43);
        assertThat(first.hash())
                .isEqualTo(new OpaqueToken(first.value()).hash())
                .hasSize(64);
        assertThat(first.toString()).doesNotContain(first.value());
    }
}
