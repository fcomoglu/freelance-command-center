package com.freelancecommandcenter;

import java.util.List;
import java.util.Map;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

/**
 * Real HTTP calls against the running API. Never throws on error statuses so tests can assert them; non-JSON bodies
 * (for example CORS rejections) come back as {@code null}.
 */
public class ApiClient {

    private static final ParameterizedTypeReference<Map<String, Object>> JSON_OBJECT =
            new ParameterizedTypeReference<>() {};

    private final RestClient client;

    ApiClient(String baseUrl) {
        this.client = RestClient.create(baseUrl);
    }

    public ResponseEntity<Map<String, Object>> post(String path, Object body) {
        return post(path, body, new HttpHeaders());
    }

    public ResponseEntity<Map<String, Object>> post(String path, Object body, HttpHeaders headers) {
        return send(client.post()
                .uri(path)
                .headers(h -> h.addAll(headers))
                .contentType(MediaType.APPLICATION_JSON)
                .body(body));
    }

    public ResponseEntity<Map<String, Object>> get(String path, HttpHeaders headers) {
        return send(client.get().uri(path).headers(h -> h.addAll(headers)));
    }

    private static ResponseEntity<Map<String, Object>> send(RestClient.RequestHeadersSpec<?> request) {
        return request.exchange((req, response) -> {
            MediaType type = response.getHeaders().getContentType();
            boolean json = type != null
                    && (type.getSubtype().equals("json") || type.getSubtype().endsWith("+json"));
            Map<String, Object> body = json ? response.bodyTo(JSON_OBJECT) : null;
            return ResponseEntity.status(response.getStatusCode())
                    .headers(response.getHeaders())
                    .body(body);
        });
    }

    /** The {@code field} of every entry in the {@code errors} array of a validation problem. */
    @SuppressWarnings("unchecked")
    public static List<String> errorFields(ResponseEntity<Map<String, Object>> response) {
        return ((List<Map<String, Object>>) response.getBody().get("errors"))
                .stream().map(error -> (String) error.get("field")).toList();
    }
}
