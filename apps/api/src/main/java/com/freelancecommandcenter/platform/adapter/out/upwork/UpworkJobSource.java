package com.freelancecommandcenter.platform.adapter.out.upwork;

import com.freelancecommandcenter.platform.api.JobListing;
import com.freelancecommandcenter.platform.application.port.out.JobSource;
import com.freelancecommandcenter.platform.application.port.out.PlatformUnavailableException;
import com.freelancecommandcenter.shared.platform.PlatformCode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Reads open jobs from the Upwork job search (GraphQL {@code marketplaceJobPostingsSearch}). The search has no "updated
 * since" filter, so it lists the newest jobs first and stops at the first job older than the watermark.
 */
class UpworkJobSource implements JobSource {

    private final RestClient client;
    private final UpworkJobTranslator translator;
    private final JsonMapper json;
    private final String query;
    private final int pageSize;
    private final int maxPages;

    UpworkJobSource(
            RestClient client,
            UpworkJobTranslator translator,
            JsonMapper json,
            String query,
            int pageSize,
            int maxPages) {
        this.client = client;
        this.translator = translator;
        this.json = json;
        this.query = query;
        this.pageSize = pageSize;
        this.maxPages = maxPages;
    }

    @Override
    public PlatformCode platform() {
        return PlatformCode.UPWORK;
    }

    @Override
    public List<FetchedListing> fetchUpdatedSince(Instant since) {
        List<FetchedListing> listings = new ArrayList<>();
        String after = null;
        for (int page = 0; page < maxPages; page++) {
            JsonNode connection = search(after);
            boolean reachedKnownJobs = false;
            for (JsonNode edge : connection.path("edges").values()) {
                JsonNode job = edge.path("node");
                Optional<JobListing> listing = translator.translate(job);
                if (listing.isEmpty()) {
                    continue;
                }
                if (listing.get().updatedAt().isBefore(since)) {
                    reachedKnownJobs = true;
                } else {
                    listings.add(new FetchedListing(listing.get(), job.toString()));
                }
            }
            JsonNode pageInfo = connection.path("pageInfo");
            after = pageInfo.path("endCursor").stringValue("");
            if (reachedKnownJobs || !pageInfo.path("hasNextPage").asBoolean(false) || after.isBlank()) {
                break;
            }
        }
        return listings;
    }

    private JsonNode search(String after) {
        Map<String, Object> pagination = new LinkedHashMap<>();
        pagination.put("first", pageSize);
        if (after != null) {
            pagination.put("after", after);
        }
        Map<String, Object> request = Map.of(
                "query",
                query,
                "variables",
                Map.of("filter", Map.of("pagination_eq", pagination), "sort", List.of(Map.of("field", "RECENCY"))));
        try {
            String body = client.post()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(json.writeValueAsString(request))
                    .retrieve()
                    .body(String.class);
            JsonNode root = json.readTree(body == null ? "" : body);
            JsonNode errors = root.path("errors");
            if (!errors.isEmpty()) {
                throw unavailable(
                        "GraphQL error: " + errors.path(0).path("message").stringValue("unknown"), null);
            }
            JsonNode connection = root.path("data").path("marketplaceJobPostingsSearch");
            if (!connection.isObject()) {
                throw unavailable("response without job search result", null);
            }
            return connection;
        } catch (RestClientResponseException exception) {
            int status = exception.getStatusCode().value();
            throw unavailable(status == 429 ? "rate limited (HTTP 429)" : "HTTP " + status, exception);
        } catch (RestClientException | JacksonException exception) {
            throw unavailable(exception.getMessage(), exception);
        }
    }

    private PlatformUnavailableException unavailable(String message, Throwable cause) {
        return new PlatformUnavailableException(platform(), message, cause);
    }
}
