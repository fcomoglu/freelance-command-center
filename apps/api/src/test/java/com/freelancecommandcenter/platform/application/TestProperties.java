package com.freelancecommandcenter.platform.application;

import java.net.URI;
import java.time.Duration;
import org.springframework.util.unit.DataSize;

/** Platform settings with the defaults of {@code application.yaml}, for use case tests. */
final class TestProperties {

    static final Duration MESSAGE_LOOKBACK = Duration.ofDays(7);
    static final Duration PROPOSAL_WINDOW = Duration.ofDays(30);

    private TestProperties() {}

    static PlatformProperties.Connections connections() {
        return new PlatformProperties.Connections(
                URI.create("http://localhost:5173"),
                "",
                new PlatformProperties.Sync(
                        true, Duration.ofMinutes(2), Duration.ZERO, MESSAGE_LOOKBACK, PROPOSAL_WINDOW));
    }

    static PlatformProperties platform() {
        return new PlatformProperties(
                new PlatformProperties.JobSync(true, Duration.ofMinutes(5), Duration.ZERO, Duration.ofHours(1)),
                new PlatformProperties.Mail("inbound.localhost", "", DataSize.ofMegabytes(5)),
                connections());
    }
}
