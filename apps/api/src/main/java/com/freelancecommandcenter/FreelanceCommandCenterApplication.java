package com.freelancecommandcenter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.modulith.Modulithic;

/** Entry point of the Freelance Command Center API, a Spring Modulith modular monolith. */
@Modulithic(sharedModules = "shared")
@SpringBootApplication
public class FreelanceCommandCenterApplication {

    public static void main(String[] args) {
        SpringApplication.run(FreelanceCommandCenterApplication.class, args);
    }
}
