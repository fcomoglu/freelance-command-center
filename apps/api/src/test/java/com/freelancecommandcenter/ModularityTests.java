package com.freelancecommandcenter;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

/** Verifies the module boundaries of the modular monolith and generates module documentation. */
class ModularityTests {

    private final ApplicationModules modules = ApplicationModules.of(FreelanceCommandCenterApplication.class);

    @Test
    void modulesRespectTheirBoundaries() {
        modules.verify();
    }

    @Test
    void writesModuleDocumentation() {
        new Documenter(modules).writeDocumentation();
    }
}
