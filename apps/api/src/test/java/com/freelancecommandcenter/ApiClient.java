package com.freelancecommandcenter;

import java.util.List;
import java.util.Map;
import java.util.UUID;
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

    /** Posts a raw body with the given content type, for example a MIME message. */
    public ResponseEntity<Map<String, Object>> postRaw(String path, byte[] body, MediaType type, HttpHeaders headers) {
        return send(client.post()
                .uri(path)
                .headers(h -> h.addAll(headers))
                .contentType(type)
                .body(body));
    }

    public ResponseEntity<Map<String, Object>> put(String path, Object body, HttpHeaders headers) {
        return send(client.put()
                .uri(path)
                .headers(h -> h.addAll(headers))
                .contentType(MediaType.APPLICATION_JSON)
                .body(body));
    }

    public ResponseEntity<Map<String, Object>> delete(String path, HttpHeaders headers) {
        return send(client.delete().uri(path).headers(h -> h.addAll(headers)));
    }

    public ResponseEntity<Map<String, Object>> get(String path, HttpHeaders headers) {
        return send(client.get().uri(path).headers(h -> h.addAll(headers)));
    }

    /** Signs up and signs in a new user; returns headers that authenticate as that user. */
    public HttpHeaders newSignedInUser() {
        return signUpAndSignIn("user-" + UUID.randomUUID() + "@example.com", null);
    }

    /** Like {@link #newSignedInUser()}, with the e-mail address confirmed through the link in the verification mail. */
    public HttpHeaders newVerifiedUser(Mailbox mailbox) {
        return signUpAndSignIn("user-" + UUID.randomUUID() + "@example.com", mailbox);
    }

    private HttpHeaders signUpAndSignIn(String email, Mailbox mailbox) {
        Map<String, String> credentials = Map.of("email", email, "password", "correct horse battery");
        post("/api/v1/auth/sign-up", credentials);
        if (mailbox != null) {
            String token =
                    mailbox.awaitMail(email, "Confirm your e-mail address").token();
            post("/api/v1/auth/email-verification", Map.of("token", token));
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(
                (String) post("/api/v1/auth/sign-in", credentials).getBody().get("accessToken"));
        return headers;
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
