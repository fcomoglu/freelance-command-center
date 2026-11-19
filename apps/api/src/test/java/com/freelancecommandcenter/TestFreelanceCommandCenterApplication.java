package com.freelancecommandcenter;

import org.springframework.boot.SpringApplication;

/** Runs the API locally with Testcontainers instead of Docker Compose: ./mvnw spring-boot:test-run */
public class TestFreelanceCommandCenterApplication {

    public static void main(String[] args) {
        SpringApplication.from(FreelanceCommandCenterApplication::main)
                .with(TestcontainersConfiguration.class)
                .run(args);
    }
}
