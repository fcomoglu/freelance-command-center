package com.freelancecommandcenter.platform.application;

import java.net.URI;
import java.time.Duration;
import org.springframework.util.unit.DataSize;

/** Platform settings with the defaults of {@code application.yaml}, for use case tests. */
final class TestProperties {

    static final Duration INITIAL_LOOKBACK = Duration.ofDays(7);

    private TestProperties() {}

    static PlatformProperties.Connections connections() {
        return new PlatformProperties.Connections(
                URI.create("http://localhost:5173"),
                "",
                new PlatformProperties.MessageSync(true, Duration.ofMinutes(2), Duration.ZERO, INITIAL_LOOKBACK));
    }

    static PlatformProperties platform() {
        return new PlatformProperties(
                new PlatformProperties.JobSync(true, Duration.ofMinutes(5), Duration.ZERO, Duration.ofHours(1)),
                new PlatformProperties.Mail("inbound.localhost", "", DataSize.ofMegabytes(5)),
                connections());
    }
}
