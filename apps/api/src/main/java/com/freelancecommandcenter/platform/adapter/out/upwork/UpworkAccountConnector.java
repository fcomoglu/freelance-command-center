package com.freelancecommandcenter.platform.adapter.out.upwork;

import com.freelancecommandcenter.platform.adapter.out.oauth.OAuthTokenClient;
import com.freelancecommandcenter.platform.application.port.out.AccountConnector;
import com.freelancecommandcenter.platform.application.port.out.PlatformUnavailableException;
import com.freelancecommandcenter.platform.domain.ConnectedAccount;
import com.freelancecommandcenter.platform.domain.OAuthTokens;
import com.freelancecommandcenter.shared.platform.PlatformCode;
import java.net.URI;
import java.util.Map;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.databind.JsonNode;

/**
 * Connects Upwork accounts through the configured API key. Upwork grants the permissions configured on the key, so
 * the consent page carries no scopes. Access tokens live 24 hours; a refresh token expires after two weeks unused,
 * which the message read prevents by renewing every day.
 */
class UpworkAccountConnector implements AccountConnector {

    /** The lifetime Upwork documents, for a token response without {@code expires_in}. */
    static final long DEFAULT_EXPIRES_IN_SECONDS = 86_400;

    private final OAuthTokenClient tokens;
    private final UpworkAccountApi api;
    private final UpworkProperties properties;
    private final String currentUserQuery;

    UpworkAccountConnector(
            OAuthTokenClient tokens, UpworkAccountApi api, UpworkProperties properties, String currentUserQuery) {
        this.tokens = tokens;
        this.api = api;
        this.properties = properties;
        this.currentUserQuery = currentUserQuery;
    }

    @Override
    public PlatformCode platform() {
        return PlatformCode.UPWORK;
    }

    @Override
    public URI authorizationUrl(String state, URI redirectUri) {
        return UriComponentsBuilder.fromUri(properties.authorizeUrl())
                .query("response_type=code&client_id={clientId}&redirect_uri={redirectUri}&state={state}")
                .encode()
                .buildAndExpand(Map.of(
                        "clientId", properties.clientId(), "redirectUri", redirectUri.toString(), "state", state))
                .toUri();
    }

    @Override
    public ConnectedAccount connect(String code, URI redirectUri) {
        OAuthTokens granted = tokens.redeem(code, redirectUri);
        JsonNode user =
                api.query(granted.accessToken(), currentUserQuery, Map.of()).path("user");
        String accountId = user.path("id").asString("");
        if (accountId.isBlank()) {
            throw new PlatformUnavailableException(platform(), "current user without id", null);
        }
        String name = user.path("name").asString("").strip();
        return new ConnectedAccount(accountId, name.isEmpty() ? accountId : name, granted);
    }

    @Override
    public OAuthTokens refresh(OAuthTokens current) {
        return tokens.refresh(current);
    }
}
