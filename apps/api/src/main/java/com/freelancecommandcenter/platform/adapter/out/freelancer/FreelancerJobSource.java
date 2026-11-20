package com.freelancecommandcenter.platform.adapter.out.freelancer;

import com.freelancecommandcenter.platform.application.port.out.JobSource;
import com.freelancecommandcenter.platform.application.port.out.PlatformUnavailableException;
import com.freelancecommandcenter.shared.platform.PlatformCode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Reads open projects from the Freelancer.com active-projects search, oldest update first, a bounded number of pages
 * per sync so one run stays within the API's rate limits.
 */
class FreelancerJobSource implements JobSource {

    static final String ACTIVE_PROJECTS_PATH = "/api/projects/0.1/projects/active/";

    private final RestClient client;
    private final FreelancerProjectTranslator translator;
    private final JsonMapper json;
    private final int pageSize;
    private final int maxPages;

    FreelancerJobSource(
            RestClient client, FreelancerProjectTranslator translator, JsonMapper json, int pageSize, int maxPages) {
        this.client = client;
        this.translator = translator;
        this.json = json;
        this.pageSize = pageSize;
        this.maxPages = maxPages;
    }

    @Override
    public PlatformCode platform() {
        return PlatformCode.FREELANCER;
    }

    @Override
    public List<FetchedListing> fetchUpdatedSince(Instant since) {
        List<FetchedListing> listings = new ArrayList<>();
        for (int page = 0; page < maxPages; page++) {
            JsonNode projects = fetchPage(since, page * pageSize);
            for (JsonNode project : projects.values()) {
                translator
                        .translate(project)
                        .ifPresent(listing -> listings.add(new FetchedListing(listing, project.toString())));
            }
            if (projects.size() < pageSize) {
                break;
            }
        }
        return listings;
    }

    private JsonNode fetchPage(Instant since, int offset) {
        try {
            String body = client.get()
                    .uri(uri -> uri.path(ACTIVE_PROJECTS_PATH)
                            .queryParam("from_time", since.getEpochSecond())
                            .queryParam("sort_field", "time_updated")
                            .queryParam("reverse_sort", true)
                            .queryParam("limit", pageSize)
                            .queryParam("offset", offset)
                            .queryParam("full_description", true)
                            .queryParam("job_details", true)
                            .queryParam("compact", true)
                            .build())
                    .retrieve()
                    .body(String.class);
            JsonNode root = json.readTree(body == null ? "" : body);
            if (!"success".equals(root.path("status").stringValue(""))) {
                throw unavailable("unexpected response status " + root.path("status"), null);
            }
            return root.path("result").path("projects");
        } catch (RestClientResponseException exception) {
            throw unavailable(describe(exception.getStatusCode().value()), exception);
        } catch (RestClientException | JacksonException exception) {
            throw unavailable(exception.getMessage(), exception);
        }
    }

    private static String describe(int status) {
        return switch (status) {
            case 401, 403 -> "access token rejected (HTTP " + status + "); generate a new personal access token";
            case 429 -> "rate limited (HTTP 429)";
            default -> "HTTP " + status;
        };
    }

    private PlatformUnavailableException unavailable(String message, Throwable cause) {
        return new PlatformUnavailableException(platform(), message, cause);
    }
}
