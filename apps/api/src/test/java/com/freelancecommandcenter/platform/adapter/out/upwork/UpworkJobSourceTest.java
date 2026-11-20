package com.freelancecommandcenter.platform.adapter.out.upwork;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
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
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

class UpworkJobSourceTest {

    private static final String API = "https://api.upwork.com/graphql";
    private static final String EMPTY_LAST_PAGE = """
            {"data":{"marketplaceJobPostingsSearch":{"edges":[],"pageInfo":{"endCursor":"","hasNextPage":false}}}}""";

    private final RestClient.Builder builder = RestClient.builder().baseUrl(API);
    private final MockRestServiceServer server =
            MockRestServiceServer.bindTo(builder).build();

    @Test
    void translatesJobsNewestFirstAndSkipsJobsWithoutATitle() {
        server.expect(requestTo(API))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.query").value(containsString("marketplaceJobPostingsSearch")))
                .andExpect(jsonPath("$.variables.filter.pagination_eq.first").value(50))
                .andExpect(jsonPath("$.variables.filter.pagination_eq.after").doesNotExist())
                .andExpect(jsonPath("$.variables.sort[0].field").value("RECENCY"))
                .andRespond(withSuccess(fixture("upwork/job-search-page-1.json"), MediaType.APPLICATION_JSON));
        server.expect(requestTo(API))
                .andExpect(jsonPath("$.variables.filter.pagination_eq.after").value("cursor-page-1"))
                .andRespond(withSuccess(EMPTY_LAST_PAGE, MediaType.APPLICATION_JSON));

        List<FetchedListing> fetched = source(4).fetchUpdatedSince(Instant.parse("2026-11-20T07:00:00Z"));

        server.verify();
        assertThat(fetched).hasSize(2);
        JobListing hourly = fetched.getFirst().listing();
        assertThat(hourly.platform()).isEqualTo(PlatformCode.UPWORK);
        assertThat(hourly.externalId()).isEqualTo("1790000000000000003");
        assertThat(hourly.title()).isEqualTo("Senior Spring Boot developer for a fintech API");
        assertThat(hourly.url()).isEqualTo(URI.create("https://www.upwork.com/jobs/~021790000000000000003"));
        assertThat(hourly.budget().type()).isEqualTo(BudgetType.HOURLY);
        assertThat(hourly.budget().minimum()).isEqualTo(Money.of("40", "USD"));
        assertThat(hourly.budget().maximum()).isEqualTo(Money.of("65", "USD"));
        assertThat(hourly.skills()).containsExactly("Java", "Spring Boot");
        assertThat(hourly.bidCount()).isEqualTo(7);
        assertThat(hourly.postedAt()).isEqualTo(Instant.parse("2026-11-20T12:00:00Z"));
        assertThat(fetched.getFirst().rawJson()).contains("\"ciphertext\":\"~021790000000000000003\"");

        JobListing fixed = fetched.get(1).listing();
        assertThat(fixed.url()).isEqualTo(URI.create("https://www.upwork.com/jobs/~021790000000000000002"));
        assertThat(fixed.budget().type()).isEqualTo(BudgetType.FIXED);
        assertThat(fixed.budget().minimum()).isEqualTo(Money.of("500", "USD"));
        assertThat(fixed.budget().maximum()).isNull();
        assertThat(fixed.skills()).containsExactly("react");
        assertThat(fixed.postedAt()).isEqualTo(Instant.parse("2026-11-20T09:00:00Z"));
        assertThat(fixed.updatedAt()).isEqualTo(Instant.parse("2026-11-20T11:00:00Z"));
    }

    @Test
    void stopsAtTheFirstJobOlderThanTheWatermark() {
        server.expect(requestTo(API))
                .andRespond(withSuccess(fixture("upwork/job-search-page-1.json"), MediaType.APPLICATION_JSON));

        List<FetchedListing> fetched = source(4).fetchUpdatedSince(Instant.parse("2026-11-20T11:30:00Z"));

        server.verify();
        assertThat(fetched).extracting(item -> item.listing().externalId()).containsExactly("1790000000000000003");
    }

    @Test
    void readsAtMostTheConfiguredNumberOfPages() {
        server.expect(requestTo(API))
                .andRespond(withSuccess(fixture("upwork/job-search-page-1.json"), MediaType.APPLICATION_JSON));

        assertThat(source(1).fetchUpdatedSince(Instant.parse("2026-11-20T07:00:00Z")))
                .hasSize(2);
        server.verify();
    }

    @Test
    void reportsGraphQlErrorsAsAnUnavailablePlatform() {
        server.expect(requestTo(API))
                .andRespond(withSuccess(
                        "{\"errors\":[{\"message\":\"Insufficient scopes\"}],\"data\":null}",
                        MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> source(4).fetchUpdatedSince(Instant.EPOCH))
                .isInstanceOf(PlatformUnavailableException.class)
                .hasMessageContaining("Insufficient scopes");
    }

    @Test
    void reportsRateLimitingAsAnUnavailablePlatform() {
        server.expect(requestTo(API)).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        assertThatThrownBy(() -> source(4).fetchUpdatedSince(Instant.EPOCH))
                .isInstanceOf(PlatformUnavailableException.class)
                .hasMessageContaining("rate limited");
    }

    private UpworkJobSource source(int maxPages) {
        return new UpworkJobSource(
                builder.build(),
                new UpworkJobTranslator(URI.create("https://www.upwork.com")),
                JsonMapper.builder().build(),
                UpworkConfiguration.query(UpworkConfiguration.JOB_SEARCH_QUERY),
                50,
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
