package com.freelancecommandcenter.identity.domain;

import java.time.Duration;

/** What a one-time token proves, and how long it stays valid. */
public enum TokenPurpose {
    EMAIL_VERIFICATION(Duration.ofHours(24)),
    PASSWORD_RESET(Duration.ofHours(1));

    private final Duration lifetime;

    TokenPurpose(Duration lifetime) {
        this.lifetime = lifetime;
    }

    public Duration lifetime() {
        return lifetime;
    }
}
