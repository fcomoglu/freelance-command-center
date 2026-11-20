package com.freelancecommandcenter.platform.adapter.out.upwork;

import com.freelancecommandcenter.platform.application.port.out.PlatformUnavailableException;
import com.freelancecommandcenter.shared.platform.PlatformCode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * The current Upwork access token. Access tokens live 24 hours; a new one is obtained with the refresh token grant
 * shortly before expiry or after the API rejected the current one.
 */
class UpworkAccessTokens {

    /** Refresh this long before expiry, so a token never runs out in the middle of a sync. */
    private static final Duration EXPIRY_MARGIN = Duration.ofMinutes(5);

    private final RestClient tokenEndpoint;
    private final UpworkProperties properties;
    private final UpworkRefreshTokenStore store;
    private final JsonMapper json;
    private final Clock clock;

    private String accessToken;
    private Instant expiresAt = Instant.MIN;

    UpworkAccessTokens(
            RestClient tokenEndpoint,
            UpworkProperties properties,
            UpworkRefreshTokenStore store,
            JsonMapper json,
            Clock clock) {
        this.tokenEndpoint = tokenEndpoint;
        this.properties = properties;
        this.store = store;
        this.json = json;
        this.clock = clock;
    }

    synchronized String current() {
        if (accessToken == null || !clock.instant().isBefore(expiresAt.minus(EXPIRY_MARGIN))) {
            refresh();
        }
        return accessToken;
    }

    /** Forgets the current token after the API rejected it. */
    synchronized void invalidate() {
        accessToken = null;
    }

    /**
     * Uses the stored refresh token; falls back to the configured one when the stored token is rejected, which is the
     * case after the API key was authorized again.
     */
    private void refresh() {
        Optional<String> stored = store.find();
        String configured = properties.refreshToken();
        if (stored.isEmpty()) {
            redeem(configured);
            return;
        }
        try {
            redeem(stored.get());
        } catch (RefreshTokenRejected rejected) {
            if (configured.isBlank() || configured.equals(stored.get())) {
                throw rejected;
            }
            redeem(configured);
        }
    }

    private void redeem(String refreshToken) {
        if (refreshToken.isBlank()) {
            throw new RefreshTokenRejected("no refresh token configured (UPWORK_REFRESH_TOKEN)", null);
        }
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "refresh_token");
        form.add("client_id", properties.clientId());
        form.add("client_secret", properties.clientSecret());
        form.add("refresh_token", refreshToken);
        JsonNode response;
        try {
            String body = tokenEndpoint
                    .post()
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(String.class);
            response = json.readTree(body == null ? "" : body);
        } catch (RestClientResponseException exception) {
            int status = exception.getStatusCode().value();
            if (status == 400 || status == 401) {
                throw new RefreshTokenRejected(
                        "refresh token rejected (HTTP " + status + "); authorize the API key again", exception);
            }
            throw new PlatformUnavailableException(PlatformCode.UPWORK, "token endpoint HTTP " + status, exception);
        } catch (RestClientException | JacksonException exception) {
            throw new PlatformUnavailableException(PlatformCode.UPWORK, "token endpoint unreachable", exception);
        }
        String newAccessToken = response.path("access_token").stringValue("");
        if (newAccessToken.isBlank()) {
            throw new PlatformUnavailableException(PlatformCode.UPWORK, "token response without access token", null);
        }
        accessToken = newAccessToken;
        expiresAt = clock.instant().plusSeconds(response.path("expires_in").asLong(3600));
        String rotated = response.path("refresh_token").stringValue("");
        store.save(rotated.isBlank() ? refreshToken : rotated);
    }

    /** The refresh token is missing, expired or revoked; only authorizing the API key again helps. */
    static final class RefreshTokenRejected extends PlatformUnavailableException {

        private static final long serialVersionUID = 1L;

        RefreshTokenRejected(String message, Throwable cause) {
            super(PlatformCode.UPWORK, message, cause);
        }
    }
}
