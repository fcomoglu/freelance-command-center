package dev.furkancomoglu.fcc;

import org.springframework.boot.SpringApplication;

/** Runs the API locally with Testcontainers instead of Docker Compose: ./gradlew bootTestRun */
public class TestFreelanceCommandCenterApplication {

    public static void main(String[] args) {
        SpringApplication.from(FreelanceCommandCenterApplication::main)
                .with(TestcontainersConfiguration.class)
                .run(args);
    }
}
