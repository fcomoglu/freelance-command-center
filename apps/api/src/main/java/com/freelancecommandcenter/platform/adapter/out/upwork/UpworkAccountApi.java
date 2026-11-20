package com.freelancecommandcenter.platform.adapter.out.upwork;

import com.freelancecommandcenter.platform.application.port.out.ConnectionRejectedException;
import com.freelancecommandcenter.platform.application.port.out.PlatformUnavailableException;
import com.freelancecommandcenter.shared.platform.PlatformCode;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** GraphQL requests on behalf of a connected account, authorized with that account's access token. */
final class UpworkAccountApi {

    private final RestClient api;
    private final JsonMapper json;

    /** @param api client of the GraphQL endpoint without a token */
    UpworkAccountApi(RestClient api, JsonMapper json) {
        this.api = api;
        this.json = json;
    }

    /**
     * @return the {@code data} of the response
     * @throws ConnectionRejectedException when Upwork rejects the access token
     * @throws PlatformUnavailableException on any other failure, including GraphQL errors
     */
    JsonNode query(String accessToken, String query, Map<String, Object> variables) {
        try {
            String body = api.post()
                    .contentType(MediaType.APPLICATION_JSON)
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .body(json.writeValueAsString(Map.of("query", query, "variables", variables)))
                    .retrieve()
                    .body(String.class);
            JsonNode root = json.readTree(body == null ? "" : body);
            JsonNode errors = root.path("errors");
            if (!errors.isEmpty()) {
                throw unavailable(
                        "GraphQL error: " + errors.path(0).path("message").asString("unknown"), null);
            }
            return root.path("data");
        } catch (RestClientResponseException exception) {
            int status = exception.getStatusCode().value();
            if (status == 401) {
                throw new ConnectionRejectedException(
                        PlatformCode.UPWORK, "access token rejected (HTTP 401)", exception);
            }
            throw unavailable(status == 429 ? "rate limited (HTTP 429)" : "HTTP " + status, exception);
        } catch (RestClientException | JacksonException exception) {
            throw unavailable(exception.getMessage(), exception);
        }
    }

    private static PlatformUnavailableException unavailable(String message, Throwable cause) {
        return new PlatformUnavailableException(PlatformCode.UPWORK, message, cause);
    }
}
