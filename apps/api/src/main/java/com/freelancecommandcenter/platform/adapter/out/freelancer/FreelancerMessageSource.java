package com.freelancecommandcenter.platform.adapter.out.freelancer;

import com.freelancecommandcenter.platform.api.PlatformMessage;
import com.freelancecommandcenter.platform.api.PlatformMessage.Detail;
import com.freelancecommandcenter.platform.api.PlatformMessage.Direction;
import com.freelancecommandcenter.platform.application.port.out.ConnectionRejectedException;
import com.freelancecommandcenter.platform.application.port.out.MessageSource;
import com.freelancecommandcenter.platform.application.port.out.PlatformUnavailableException;
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
 * Reads a connected account's messages from the Freelancer.com messaging API (scope {@code fln:messaging}), with the
 * threads and users they belong to, so each message carries its conversation and the other party's name.
 */
class FreelancerMessageSource implements MessageSource {

    static final String MESSAGES_PATH = "/api/messages/0.1/messages/";

    private final RestClient api;
    private final URI site;
    private final JsonMapper json;
    private final int pageSize;
    private final int maxPages;

    FreelancerMessageSource(RestClient api, URI site, JsonMapper json, int pageSize, int maxPages) {
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
    public List<PlatformMessage> fetchSince(String accountId, String accessToken, Instant since) {
        List<PlatformMessage> messages = new ArrayList<>();
        for (int page = 0; page < maxPages; page++) {
            JsonNode result = fetchPage(accessToken, since, page * pageSize);
            JsonNode items = result.path("messages");
            for (JsonNode item : items.values()) {
                translate(item, result, accountId).ifPresent(messages::add);
            }
            if (items.size() < pageSize) {
                break;
            }
        }
        return messages;
    }

    private JsonNode fetchPage(String accessToken, Instant since, int offset) {
        try {
            String body = api.get()
                    .uri(uri -> uri.path(MESSAGES_PATH)
                            .queryParam("from_updated_time", since.getEpochSecond())
                            .queryParam("thread_details", true)
                            .queryParam("user_details", true)
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

    /** Skips entries without the fields that identify a message. */
    private Optional<PlatformMessage> translate(JsonNode item, JsonNode result, String accountId) {
        String id = item.path("id").asString("");
        String threadId = item.path("thread_id").asString("");
        JsonNode created = item.path("time_created");
        if (id.isBlank() || threadId.isBlank() || !created.isNumber()) {
            return Optional.empty();
        }
        String from = item.path("from_user").asString("");
        Direction direction = from.equals(accountId) ? Direction.OUTBOUND : Direction.INBOUND;
        return Optional.of(new PlatformMessage(
                PlatformCode.FREELANCER,
                threadId,
                id,
                counterpart(result, threadId, accountId, direction == Direction.INBOUND ? from : ""),
                direction,
                Detail.FULL,
                item.path("message").asString("").strip(),
                site.resolve("/messages/thread/" + threadId),
                Instant.ofEpochSecond(created.longValue())));
    }

    /**
     * The thread's first member other than the account, else the sender of an inbound message; named by the user
     * details of the response, else by user id.
     */
    private static String counterpart(JsonNode result, String threadId, String accountId, String sender) {
        JsonNode thread = entry(result.path("threads"), threadId);
        JsonNode members = thread.has("thread") ? thread.path("thread").path("members") : thread.path("members");
        String other = sender;
        for (JsonNode member : members.values()) {
            String memberId = member.asString("");
            if (!memberId.isBlank() && !memberId.equals(accountId)) {
                other = memberId;
                break;
            }
        }
        if (other.isBlank()) {
            return "Freelancer.com";
        }
        String name = FreelancerAccountConnector.displayName(entry(result.path("users"), other));
        return name.isBlank() ? other : name;
    }

    /** Projections arrive as an object keyed by id or as an array of objects with an {@code id}. */
    static JsonNode entry(JsonNode container, String id) {
        if (container.isObject()) {
            return container.path(id);
        }
        for (JsonNode element : container.values()) {
            if (id.equals(element.path("id").asString(""))) {
                return element;
            }
        }
        return container.path(id);
    }
}
