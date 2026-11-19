package com.freelancecommandcenter;

import java.util.List;
import java.util.Map;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

/** Real HTTP calls against the running API; never throws on error statuses so tests can assert them. */
public class ApiClient {

    private static final ParameterizedTypeReference<Map<String, Object>> JSON_OBJECT =
            new ParameterizedTypeReference<>() {};

    private final RestClient client;

    ApiClient(String baseUrl) {
        this.client = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultStatusHandler(status -> true, (request, response) -> {})
                .build();
    }

    public ResponseEntity<Map<String, Object>> post(String path, Object body) {
        return post(path, body, new HttpHeaders());
    }

    public ResponseEntity<Map<String, Object>> post(String path, Object body, HttpHeaders headers) {
        return client.post()
                .uri(path)
                .headers(h -> h.addAll(headers))
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toEntity(JSON_OBJECT);
    }

    public ResponseEntity<Map<String, Object>> get(String path, HttpHeaders headers) {
        return client.get().uri(path).headers(h -> h.addAll(headers)).retrieve().toEntity(JSON_OBJECT);
    }

    /** The {@code field} of every entry in the {@code errors} array of a validation problem. */
    @SuppressWarnings("unchecked")
    public static List<String> errorFields(ResponseEntity<Map<String, Object>> response) {
        return ((List<Map<String, Object>>) response.getBody().get("errors"))
                .stream().map(error -> (String) error.get("field")).toList();
    }
}
