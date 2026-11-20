package com.freelancecommandcenter;

import com.freelancecommandcenter.platform.application.port.out.AccountConnector;
import com.freelancecommandcenter.platform.application.port.out.ConnectionRejectedException;
import com.freelancecommandcenter.platform.domain.ConnectedAccount;
import com.freelancecommandcenter.platform.domain.OAuthTokens;
import com.freelancecommandcenter.shared.platform.PlatformCode;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;

/**
 * Stands in for the Freelancer.com OAuth app in integration tests. The consent page is {@link #CONSENT_PAGE} with the
 * state as its only parameter; every code connects the account {@code 1001} with the access token
 * {@code access-<code>}, except {@link #REFUSED_CODE}.
 */
public class FakeAccountConnector implements AccountConnector {

    public static final String CONSENT_PAGE = "https://accounts.example/authorize";
    public static final String REFUSED_CODE = "refused";
    public static final String ACCOUNT_ID = "1001";

    @Override
    public PlatformCode platform() {
        return PlatformCode.FREELANCER;
    }

    @Override
    public URI authorizationUrl(String state, URI redirectUri) {
        return URI.create(CONSENT_PAGE + "?state=" + state);
    }

    @Override
    public ConnectedAccount connect(String code, URI redirectUri) {
        if (REFUSED_CODE.equals(code)) {
            throw new ConnectionRejectedException(platform(), "code refused", null);
        }
        return new ConnectedAccount(
                ACCOUNT_ID,
                "Ada L.",
                new OAuthTokens(
                        "access-" + code, "refresh-" + code, Instant.now().plus(Duration.ofDays(30))));
    }

    @Override
    public OAuthTokens refresh(OAuthTokens tokens) {
        return tokens;
    }
}
