package com.freelancecommandcenter.platform.adapter.out.oauth;

import com.freelancecommandcenter.platform.application.port.out.ConnectionRejectedException;
import com.freelancecommandcenter.platform.application.port.out.PlatformUnavailableException;
import com.freelancecommandcenter.platform.domain.OAuthTokens;
import com.freelancecommandcenter.shared.platform.PlatformCode;
import java.net.URI;
import java.time.Clock;
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
 * The token endpoint of a platform's OAuth app (RFC 6749): redeems authorization codes and refresh tokens of
 * connected accounts. A refused grant means the user has to connect again; any other failure is temporary.
 */
public final class OAuthTokenClient {

    private final PlatformCode platform;
    private final RestClient tokenEndpoint;
    private final String clientId;
    private final String clientSecret;
    private final long defaultExpiresInSeconds;
    private final JsonMapper json;
    private final Clock clock;

    /**
     * @param tokenEndpoint client whose base URL is the token endpoint itself
     * @param defaultExpiresInSeconds the lifetime the platform documents, for a response without {@code expires_in}
     */
    public OAuthTokenClient(
            PlatformCode platform,
            RestClient tokenEndpoint,
            String clientId,
            String clientSecret,
            long defaultExpiresInSeconds,
            JsonMapper json,
            Clock clock) {
        this.platform = platform;
        this.tokenEndpoint = tokenEndpoint;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.defaultExpiresInSeconds = defaultExpiresInSeconds;
        this.json = json;
        this.clock = clock;
    }

    public OAuthTokens redeem(String code, URI redirectUri) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("code", code);
        form.add("redirect_uri", redirectUri.toString());
        return request(form, "");
    }

    /** Keeps the current refresh token when the platform issues no new one. */
    public OAuthTokens refresh(OAuthTokens tokens) {
        if (tokens.refreshToken().isBlank()) {
            throw new ConnectionRejectedException(platform, "no refresh token", null);
        }
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "refresh_token");
        form.add("refresh_token", tokens.refreshToken());
        return request(form, tokens.refreshToken());
    }

    private OAuthTokens request(MultiValueMap<String, String> form, String currentRefreshToken) {
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
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
                throw new ConnectionRejectedException(
                        platform, "token endpoint refused the grant (HTTP " + status + ")", exception);
            }
            throw new PlatformUnavailableException(platform, "token endpoint HTTP " + status, exception);
        } catch (RestClientException | JacksonException exception) {
            throw new PlatformUnavailableException(platform, "token endpoint unreachable", exception);
        }
        String accessToken = response.path("access_token").asString("");
        if (accessToken.isBlank()) {
            throw new PlatformUnavailableException(platform, "token response without access token", null);
        }
        String refreshToken = response.path("refresh_token").asString("");
        return new OAuthTokens(
                accessToken,
                refreshToken.isBlank() ? currentRefreshToken : refreshToken,
                clock.instant().plusSeconds(response.path("expires_in").asLong(defaultExpiresInSeconds)));
    }
}
