package com.freelancecommandcenter;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Testcontainers;

/** Starts the full application against real PostgreSQL and Redis containers. Skipped when Docker is unavailable. */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Testcontainers(disabledWithoutDocker = true)
class ApplicationContextIT {

    @Test
    void contextLoads() {}
}
