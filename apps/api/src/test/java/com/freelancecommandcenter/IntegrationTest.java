package com.freelancecommandcenter;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * The full application on a random port against real containers. All integration tests use this one configuration so
 * they share a single application context and one set of containers. Skipped when Docker is unavailable.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
// All test requests come from one IP address; per-IP limiting is covered by AuthRateLimitFilterTest. Tests start the
// job sync themselves, with FakeJobSource in place of the real platform adapters.
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
            "fcc.identity.auth-requests-per-ip-per-minute=100000",
            "fcc.platform.job-sync.enabled=false",
            "fcc.platform.freelancer.access-token=",
            "fcc.platform.upwork.client-id=",
            "fcc.platform.mail.webhook-secret=test-inbound-secret"
        })
@Import({TestcontainersConfiguration.class, TestSupportConfiguration.class})
@Testcontainers(disabledWithoutDocker = true)
public @interface IntegrationTest {}
