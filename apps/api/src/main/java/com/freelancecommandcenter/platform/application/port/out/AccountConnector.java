package com.freelancecommandcenter.platform.application.port.out;

import com.freelancecommandcenter.platform.domain.ConnectedAccount;
import com.freelancecommandcenter.platform.domain.OAuthTokens;
import com.freelancecommandcenter.shared.platform.PlatformCode;
import java.net.URI;

/** The OAuth 2 authorization code flow of one platform, through which a user connects their account (ADR 0014). */
public interface AccountConnector {

    PlatformCode platform();

    /** The platform's consent page; it sends the user back to {@code redirectUri} with a code and the state. */
    URI authorizationUrl(String state, URI redirectUri);

    /**
     * Redeems the code from the consent page and identifies the account.
     *
     * @throws ConnectionRejectedException when the platform refuses the code
     * @throws PlatformUnavailableException when the platform cannot be reached or answers unexpectedly
     */
    ConnectedAccount connect(String code, URI redirectUri);

    /**
     * New tokens for an account whose access token is about to expire.
     *
     * @throws ConnectionRejectedException when the platform no longer accepts the refresh token
     * @throws PlatformUnavailableException when the platform cannot be reached or answers unexpectedly
     */
    OAuthTokens refresh(OAuthTokens tokens);
}
