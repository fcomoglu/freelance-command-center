package com.freelancecommandcenter.platform.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * The OAuth tokens of a connected platform account. {@link #toString()} leaves the token values out, so they never
 * reach a log.
 *
 * @param refreshToken empty when the platform issued none
 */
public record OAuthTokens(String accessToken, String refreshToken, Instant expiresAt) {

    public OAuthTokens {
        if (accessToken == null || accessToken.isBlank()) {
            throw new IllegalArgumentException("accessToken must not be blank");
        }
        Objects.requireNonNull(refreshToken, "refreshToken");
        Objects.requireNonNull(expiresAt, "expiresAt");
    }

    public boolean expiresWithin(Duration margin, Instant now) {
        return !now.isBefore(expiresAt.minus(margin));
    }

    @Override
    public String toString() {
        return "OAuthTokens[expiresAt=" + expiresAt + "]";
    }
}
