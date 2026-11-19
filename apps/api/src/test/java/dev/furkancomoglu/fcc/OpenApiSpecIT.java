package dev.furkancomoglu.fcc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Writes the OpenAPI document to {@code packages/api-client/openapi.json}. CI fails when the committed file differs,
 * so every contract change shows up in the pull request.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Testcontainers(disabledWithoutDocker = true)
class OpenApiSpecIT {

    private static final Path SPEC = Path.of("../../packages/api-client/openapi.json");

    @Autowired
    private WebApplicationContext context;

    @Test
    void writesSpecForTheApiClient() throws Exception {
        MockMvc mvc = MockMvcBuilders.webAppContextSetup(context).build();

        String spec = mvc.perform(get("/api/openapi")).andReturn().getResponse().getContentAsString();

        assertThat(spec).contains("\"openapi\"");
        Files.writeString(SPEC, spec.endsWith("\n") ? spec : spec + "\n");
    }
}
