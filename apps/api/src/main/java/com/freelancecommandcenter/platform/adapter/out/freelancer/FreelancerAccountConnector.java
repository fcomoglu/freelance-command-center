package com.freelancecommandcenter.platform.adapter.out.freelancer;

import com.freelancecommandcenter.platform.adapter.out.oauth.OAuthTokenClient;
import com.freelancecommandcenter.platform.application.port.out.AccountConnector;
import com.freelancecommandcenter.platform.application.port.out.PlatformUnavailableException;
import com.freelancecommandcenter.platform.domain.ConnectedAccount;
import com.freelancecommandcenter.platform.domain.OAuthTokens;
import com.freelancecommandcenter.shared.platform.PlatformCode;
import java.net.URI;
import java.util.Map;
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
    static final long DEFAULT_EXPIRES_IN_SECONDS = 2_592_000;

    private final OAuthTokenClient tokens;
    private final RestClient api;
    private final FreelancerProperties properties;
    private final JsonMapper json;

    /** @param api client of the API host without a token; each call sends the connected account's token */
    FreelancerAccountConnector(
            OAuthTokenClient tokens, RestClient api, FreelancerProperties properties, JsonMapper json) {
        this.tokens = tokens;
        this.api = api;
        this.properties = properties;
        this.json = json;
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
        OAuthTokens granted = tokens.redeem(code, redirectUri);
        JsonNode self = self(granted.accessToken());
        String accountId = self.path("id").asString("");
        if (accountId.isBlank()) {
            throw new PlatformUnavailableException(platform(), "user without id", null);
        }
        return new ConnectedAccount(accountId, displayName(self), granted);
    }

    @Override
    public OAuthTokens refresh(OAuthTokens current) {
        return tokens.refresh(current);
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
