package com.freelancecommandcenter.platform.adapter.out.freelancer;

import com.freelancecommandcenter.platform.application.port.out.AccountConnector;
import com.freelancecommandcenter.platform.application.port.out.ConnectionRejectedException;
import com.freelancecommandcenter.platform.application.port.out.PlatformUnavailableException;
import com.freelancecommandcenter.platform.domain.ConnectedAccount;
import com.freelancecommandcenter.platform.domain.OAuthTokens;
import com.freelancecommandcenter.shared.platform.PlatformCode;
import java.net.URI;
import java.time.Clock;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Connects Freelancer.com accounts through the OAuth app configured in {@code fcc.platform.freelancer.client-id}.
 * Access tokens live 30 days; the refresh token grant renews them.
 */
class FreelancerAccountConnector implements AccountConnector {

    static final String AUTHORIZE_PATH = "/oauth/authorize";
    static final String TOKEN_PATH = "/oauth/token";
    static final String SELF_PATH = "/api/users/0.1/self/";

    /** The lifetime Freelancer.com documents, for a token response without {@code expires_in}. */
    private static final long DEFAULT_EXPIRES_IN_SECONDS = 2_592_000;

    private final RestClient accounts;
    private final RestClient api;
    private final FreelancerProperties properties;
    private final JsonMapper json;
    private final Clock clock;

    /**
     * @param accounts client of the OAuth host
     * @param api client of the API host without a token; each call sends the connected account's token
     */
    FreelancerAccountConnector(
            RestClient accounts, RestClient api, FreelancerProperties properties, JsonMapper json, Clock clock) {
        this.accounts = accounts;
        this.api = api;
        this.properties = properties;
        this.json = json;
        this.clock = clock;
    }

    @Override
    public PlatformCode platform() {
        return PlatformCode.FREELANCER;
    }

    @Override
    public URI authorizationUrl(String state, URI redirectUri) {
        return UriComponentsBuilder.fromUri(properties.accountsUrl())
                .path(AUTHORIZE_PATH)
                .query("response_type=code&client_id={clientId}&redirect_uri={redirectUri}&scope=basic"
                        + "&advanced_scopes={scopes}&prompt={prompt}&state={state}")
                .encode()
                .buildAndExpand(Map.of(
                        "clientId", properties.clientId(),
                        "redirectUri", redirectUri.toString(),
                        "scopes", properties.advancedScopes(),
                        "prompt", "select_account consent",
                        "state", state))
                .toUri();
    }

    @Override
    public ConnectedAccount connect(String code, URI redirectUri) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("code", code);
        form.add("redirect_uri", redirectUri.toString());
        OAuthTokens tokens = requestTokens(form, "");
        JsonNode self = self(tokens.accessToken());
        String accountId = self.path("id").asString("");
        if (accountId.isBlank()) {
            throw new PlatformUnavailableException(platform(), "user without id", null);
        }
        return new ConnectedAccount(accountId, displayName(self), tokens);
    }

    @Override
    public OAuthTokens refresh(OAuthTokens tokens) {
        if (tokens.refreshToken().isBlank()) {
            throw new ConnectionRejectedException(platform(), "no refresh token", null);
        }
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "refresh_token");
        form.add("refresh_token", tokens.refreshToken());
        return requestTokens(form, tokens.refreshToken());
    }

    /** @param currentRefreshToken kept when the response carries no new refresh token */
    private OAuthTokens requestTokens(MultiValueMap<String, String> form, String currentRefreshToken) {
        form.add("client_id", properties.clientId());
        form.add("client_secret", properties.clientSecret());
        JsonNode response;
        try {
            String body = accounts.post()
                    .uri(TOKEN_PATH)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(String.class);
            response = json.readTree(body == null ? "" : body);
        } catch (RestClientResponseException exception) {
            int status = exception.getStatusCode().value();
            if (status == 400 || status == 401) {
                throw new ConnectionRejectedException(
                        platform(), "token endpoint refused the grant (HTTP " + status + ")", exception);
            }
            throw new PlatformUnavailableException(platform(), "token endpoint HTTP " + status, exception);
        } catch (RestClientException | JacksonException exception) {
            throw new PlatformUnavailableException(platform(), "token endpoint unreachable", exception);
        }
        String accessToken = response.path("access_token").asString("");
        if (accessToken.isBlank()) {
            throw new PlatformUnavailableException(platform(), "token response without access token", null);
        }
        String refreshToken = response.path("refresh_token").asString("");
        long expiresIn = response.path("expires_in").asLong(DEFAULT_EXPIRES_IN_SECONDS);
        return new OAuthTokens(
                accessToken,
                refreshToken.isBlank() ? currentRefreshToken : refreshToken,
                clock.instant().plusSeconds(expiresIn));
    }

    private JsonNode self(String accessToken) {
        try {
            String body = api.get()
                    .uri(SELF_PATH)
                    .header(FreelancerConfiguration.TOKEN_HEADER, accessToken)
                    .retrieve()
                    .body(String.class);
            return json.readTree(body == null ? "" : body).path("result");
        } catch (RestClientResponseException exception) {
            throw new PlatformUnavailableException(
                    platform(),
                    "user endpoint HTTP " + exception.getStatusCode().value(),
                    exception);
        } catch (RestClientException | JacksonException exception) {
            throw new PlatformUnavailableException(platform(), "user endpoint unreachable", exception);
        }
    }

    static String displayName(JsonNode user) {
        for (String field : new String[] {"public_name", "display_name", "username"}) {
            String name = user.path(field).asString("").strip();
            if (!name.isEmpty()) {
                return name;
            }
        }
        return user.path("id").asString("");
    }
}
