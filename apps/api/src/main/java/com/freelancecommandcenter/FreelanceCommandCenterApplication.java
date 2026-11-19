package com.freelancecommandcenter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.modulith.Modulithic;
import org.springframework.scheduling.annotation.EnableAsync;

/** Entry point of the Freelance Command Center API, a Spring Modulith modular monolith. */
@Modulithic(sharedModules = "shared")
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableAsync
public class FreelanceCommandCenterApplication {

    public static void main(String[] args) {
        SpringApplication.run(FreelanceCommandCenterApplication.class, args);
    }
}
