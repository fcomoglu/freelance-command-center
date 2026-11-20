package com.freelancecommandcenter.job;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.freelancecommandcenter.ApiClient;
import com.freelancecommandcenter.FakeJobSource;
import com.freelancecommandcenter.IntegrationTest;
import com.freelancecommandcenter.platform.api.JobListing;
import com.freelancecommandcenter.platform.api.JobListing.Budget;
import com.freelancecommandcenter.platform.api.JobListing.BudgetType;
import com.freelancecommandcenter.platform.application.JobSyncUseCase;
import com.freelancecommandcenter.shared.money.Money;
import com.freelancecommandcenter.shared.platform.PlatformCode;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/** From a platform sync through the outbox into the catalog and out of the feed endpoint. */
@IntegrationTest
class JobFeedIT {

    // Later than anything else the shared test database holds, so these jobs lead the feed.
    private static final Instant FUTURE = Instant.parse("2100-01-01T00:00:00Z");

    @Autowired
    private ApiClient api;

    @Autowired
    private FakeJobSource platform;

    @Autowired
    private JobSyncUseCase jobSync;

    @Test
    void syncedListingsAppearInTheFeedNewestFirstPageByPage() {
        HttpHeaders user = api.newSignedInUser();
        String run = UUID.randomUUID().toString();
        platform.offer(
                listing(run + "-1", FUTURE.plusSeconds(1)),
                listing(run + "-2", FUTURE.plusSeconds(2)),
                listing(run + "-3", FUTURE.plusSeconds(3)));

        jobSync.syncAll();

        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> assertThat(titles(api.get("/api/v1/jobs?limit=3", user)))
                        .containsExactly(run + "-3", run + "-2", run + "-1"));

        ResponseEntity<Map<String, Object>> first = api.get("/api/v1/jobs?limit=2&platform=freelancer", user);
        assertThat(titles(first)).containsExactly(run + "-3", run + "-2");
        assertThat(items(first).getFirst())
                .containsEntry("platform", "freelancer")
                .containsEntry("url", "https://www.freelancer.com/projects/" + run + "-3")
                .containsEntry("skills", List.of("Java", "Spring Boot"))
                .containsEntry("postedAt", "2100-01-01T00:00:03Z")
                .containsEntry(
                        "budget",
                        Map.of(
                                "type",
                                "fixed",
                                "minimum",
                                Map.of("amount", "250.00", "currency", "USD"),
                                "maximum",
                                Map.of("amount", "750.00", "currency", "USD")));

        ResponseEntity<Map<String, Object>> second = api.get(
                "/api/v1/jobs?limit=2&platform=freelancer&cursor="
                        + first.getBody().get("nextCursor"),
                user);
        assertThat(titles(second)).first().isEqualTo(run + "-1");
    }

    @Test
    void theFeedRequiresASignedInUser() {
        ResponseEntity<Map<String, Object>> response = api.get("/api/v1/jobs", new HttpHeaders());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void invalidPagingParametersAreRejected() {
        HttpHeaders user = api.newSignedInUser();

        ResponseEntity<Map<String, Object>> tooMany = api.get("/api/v1/jobs?limit=101", user);
        assertThat(tooMany.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(tooMany.getBody()).containsEntry("code", "validation.failed");
        assertThat(ApiClient.errorFields(tooMany)).containsExactly("limit");

        ResponseEntity<Map<String, Object>> badCursor = api.get("/api/v1/jobs?cursor=not-a-cursor", user);
        assertThat(badCursor.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(badCursor.getBody()).containsEntry("code", "pagination.invalid_cursor");
    }

    private static JobListing listing(String title, Instant postedAt) {
        return new JobListing(
                PlatformCode.FREELANCER,
                title,
                title,
                "Build a REST API",
                URI.create("https://www.freelancer.com/projects/" + title),
                new Budget(BudgetType.FIXED, Money.of("250", "USD"), Money.of("750", "USD")),
                List.of("Java", "Spring Boot"),
                4,
                "en",
                postedAt,
                postedAt);
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> items(ResponseEntity<Map<String, Object>> response) {
        return (List<Map<String, Object>>) response.getBody().get("items");
    }

    private static List<String> titles(ResponseEntity<Map<String, Object>> response) {
        return items(response).stream().map(item -> (String) item.get("title")).toList();
    }
}
