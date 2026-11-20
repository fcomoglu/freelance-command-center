package com.freelancecommandcenter.platform.adapter.out.freelancer;

import com.freelancecommandcenter.platform.application.port.out.ConnectionRejectedException;
import com.freelancecommandcenter.platform.application.port.out.PlatformUnavailableException;
import com.freelancecommandcenter.platform.application.port.out.ProposalStatusTracker;
import com.freelancecommandcenter.platform.domain.ProposalSnapshot;
import com.freelancecommandcenter.platform.domain.ProposalStatus;
import com.freelancecommandcenter.shared.platform.PlatformCode;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Reads the bids of a connected account from the Freelancer.com projects API (scope {@code fln:project_manage}), with
 * the projects they are for. The API only reads; bids are placed on the platform.
 */
class FreelancerProposalStatusTracker implements ProposalStatusTracker {

    static final String BIDS_PATH = "/api/projects/0.1/bids/";

    private final RestClient api;
    private final URI site;
    private final JsonMapper json;
    private final int pageSize;
    private final int maxPages;

    FreelancerProposalStatusTracker(RestClient api, URI site, JsonMapper json, int pageSize, int maxPages) {
        this.api = api;
        this.site = site;
        this.json = json;
        this.pageSize = pageSize;
        this.maxPages = maxPages;
    }

    @Override
    public PlatformCode platform() {
        return PlatformCode.FREELANCER;
    }

    @Override
    public List<ProposalSnapshot> fetchSubmittedSince(String accountId, String accessToken, Instant submittedSince) {
        List<ProposalSnapshot> proposals = new ArrayList<>();
        for (int page = 0; page < maxPages; page++) {
            JsonNode result = fetchPage(accountId, accessToken, submittedSince, page * pageSize);
            JsonNode bids = result.path("bids");
            for (JsonNode bid : bids.values()) {
                translate(bid, result).ifPresent(proposals::add);
            }
            if (bids.size() < pageSize) {
                break;
            }
        }
        return proposals;
    }

    private JsonNode fetchPage(String accountId, String accessToken, Instant submittedSince, int offset) {
        try {
            String body = api.get()
                    .uri(uri -> uri.path(BIDS_PATH)
                            .queryParam("bidders[]", accountId)
                            .queryParam("from_time", submittedSince.getEpochSecond())
                            .queryParam("project_details", true)
                            .queryParam("limit", pageSize)
                            .queryParam("offset", offset)
                            .build())
                    .header(FreelancerConfiguration.TOKEN_HEADER, accessToken)
                    .retrieve()
                    .body(String.class);
            JsonNode root = json.readTree(body == null ? "" : body);
            if (!"success".equals(root.path("status").asString(""))) {
                throw new PlatformUnavailableException(
                        platform(), "unexpected response status " + root.path("status"), null);
            }
            return root.path("result");
        } catch (RestClientResponseException exception) {
            int status = exception.getStatusCode().value();
            if (status == 401 || status == 403) {
                throw new ConnectionRejectedException(
                        platform(), "access token rejected (HTTP " + status + ")", exception);
            }
            throw new PlatformUnavailableException(platform(), "HTTP " + status, exception);
        } catch (RestClientException | JacksonException exception) {
            throw new PlatformUnavailableException(platform(), exception.getMessage(), exception);
        }
    }

    /** Skips entries without the fields that identify a bid and its project. */
    private Optional<ProposalSnapshot> translate(JsonNode bid, JsonNode result) {
        String id = bid.path("id").asString("");
        String projectId = bid.path("project_id").asString("");
        if (id.isBlank() || projectId.isBlank()) {
            return Optional.empty();
        }
        JsonNode project = FreelancerMessageSource.entry(result.path("projects"), projectId);
        String title = project.path("title").asString("").strip();
        String seoUrl = project.path("seo_url").asString("").strip();
        ProposalStatus status = status(bid);
        JsonNode awarded = bid.path("time_awarded");
        return Optional.of(new ProposalSnapshot(
                id,
                title.isEmpty() ? "#" + projectId : title,
                site.resolve("/projects/" + (seoUrl.isEmpty() ? projectId : seoUrl)),
                status,
                status == ProposalStatus.AWARDED && awarded.isNumber()
                        ? Optional.of(Instant.ofEpochSecond(awarded.longValue()))
                        : Optional.empty()));
    }

    /**
     * The award status decides once the client has acted; before that, {@code shortlisted} tells a shortlisted bid from
     * a pending one.
     */
    static ProposalStatus status(JsonNode bid) {
        if (bid.path("retracted").asBoolean(false)) {
            return ProposalStatus.WITHDRAWN;
        }
        return switch (bid.path("award_status").asString("")) {
            case "awarded" -> ProposalStatus.AWARDED;
            case "rejected" -> ProposalStatus.REJECTED;
            case "revoked" -> ProposalStatus.REVOKED;
            case "canceled" -> ProposalStatus.CLOSED;
            default -> bid.path("shortlisted").asBoolean(false) ? ProposalStatus.SHORTLISTED : ProposalStatus.PENDING;
        };
    }
}
