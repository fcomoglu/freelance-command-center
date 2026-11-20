package com.freelancecommandcenter.platform.adapter.out.freelancer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.freelancecommandcenter.platform.api.JobListing;
import com.freelancecommandcenter.platform.api.JobListing.BudgetType;
import com.freelancecommandcenter.platform.application.port.out.JobSource.FetchedListing;
import com.freelancecommandcenter.platform.application.port.out.PlatformUnavailableException;
import com.freelancecommandcenter.shared.money.Money;
import com.freelancecommandcenter.shared.platform.PlatformCode;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

class FreelancerJobSourceTest {

    private static final String SITE = "https://www.freelancer.com";
    private static final Instant SINCE = Instant.ofEpochSecond(1795167000);

    private final RestClient.Builder builder =
            RestClient.builder().baseUrl(SITE).defaultHeader(FreelancerConfiguration.TOKEN_HEADER, "test-token");
    private final MockRestServiceServer server =
            MockRestServiceServer.bindTo(builder).build();

    @Test
    void translatesActiveProjectsAndKeepsTheirRawPayload() {
        server.expect(requestTo(startsWith(SITE + FreelancerJobSource.ACTIVE_PROJECTS_PATH)))
                .andExpect(header(FreelancerConfiguration.TOKEN_HEADER, "test-token"))
                .andExpect(queryParam("from_time", "1795167000"))
                .andExpect(queryParam("sort_field", "time_updated"))
                .andExpect(queryParam("reverse_sort", "true"))
                .andExpect(queryParam("offset", "0"))
                .andExpect(queryParam("full_description", "true"))
                .andExpect(queryParam("job_details", "true"))
                .andRespond(withSuccess(fixture("freelancer/active-projects.json"), MediaType.APPLICATION_JSON));

        List<FetchedListing> fetched = source(100, 5).fetchUpdatedSince(SINCE);

        server.verify();
        assertThat(fetched).hasSize(2);
        JobListing fixed = fetched.getFirst().listing();
        assertThat(fixed.platform()).isEqualTo(PlatformCode.FREELANCER);
        assertThat(fixed.externalId()).isEqualTo("39001001");
        assertThat(fixed.title()).isEqualTo("Spring Boot REST API for a booking app");
        assertThat(fixed.description()).isEqualTo("We need a REST API with Spring Boot and PostgreSQL.");
        assertThat(fixed.url()).isEqualTo(URI.create(SITE + "/projects/java/Spring-Boot-REST-API-booking"));
        assertThat(fixed.budget().type()).isEqualTo(BudgetType.FIXED);
        assertThat(fixed.budget().minimum()).isEqualTo(Money.of("250", "USD"));
        assertThat(fixed.budget().maximum()).isEqualTo(Money.of("750", "USD"));
        assertThat(fixed.skills()).containsExactly("Java", "Spring Boot");
        assertThat(fixed.bidCount()).isEqualTo(12);
        assertThat(fixed.language()).isEqualTo("en");
        assertThat(fixed.postedAt()).isEqualTo(Instant.ofEpochSecond(1795167000));
        assertThat(fixed.updatedAt()).isEqualTo(Instant.ofEpochSecond(1795167300));
        assertThat(fetched.getFirst().rawJson()).contains("\"id\":39001001");
    }

    @Test
    void fillsGapsOfSparseProjectsAndSkipsProjectsWithoutATitle() {
        server.expect(requestTo(startsWith(SITE)))
                .andRespond(withSuccess(fixture("freelancer/active-projects.json"), MediaType.APPLICATION_JSON));

        JobListing hourly = source(100, 5).fetchUpdatedSince(SINCE).get(1).listing();

        assertThat(hourly.description()).isEqualTo("Laravel ile yönetim paneli");
        assertThat(hourly.budget().type()).isEqualTo(BudgetType.HOURLY);
        assertThat(hourly.budget().minimum()).isEqualTo(Money.of("300", "TRY"));
        assertThat(hourly.budget().maximum()).isNull();
        assertThat(hourly.skills()).isEmpty();
        assertThat(hourly.bidCount()).isNull();
        assertThat(hourly.updatedAt()).isEqualTo(hourly.postedAt());
    }

    @Test
    void readsFurtherPagesOnlyWhileTheyAreFullAndAtMostTheConfiguredNumber() {
        server.expect(ExpectedCount.times(2), requestTo(startsWith(SITE)))
                .andRespond(withSuccess(fixture("freelancer/active-projects.json"), MediaType.APPLICATION_JSON));

        List<FetchedListing> fetched = source(3, 2).fetchUpdatedSince(SINCE);

        server.verify();
        assertThat(fetched).hasSize(4);
    }

    @Test
    void reportsARejectedTokenAsAnUnavailablePlatform() {
        server.expect(requestTo(startsWith(SITE))).andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> source(100, 5).fetchUpdatedSince(SINCE))
                .isInstanceOf(PlatformUnavailableException.class)
                .hasMessageContaining("access token rejected");
    }

    @Test
    void reportsAnErrorResponseAsAnUnavailablePlatform() {
        server.expect(requestTo(startsWith(SITE)))
                .andRespond(withSuccess("{\"status\":\"error\"}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> source(100, 5).fetchUpdatedSince(SINCE))
                .isInstanceOf(PlatformUnavailableException.class)
                .hasMessageContaining("unexpected response status");
    }

    private FreelancerJobSource source(int pageSize, int maxPages) {
        return new FreelancerJobSource(
                builder.build(),
                new FreelancerProjectTranslator(URI.create(SITE)),
                JsonMapper.builder().build(),
                pageSize,
                maxPages);
    }

    private static String fixture(String path) {
        try {
            return new ClassPathResource(path).getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }
}
