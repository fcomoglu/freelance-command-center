package com.freelancecommandcenter.platform.domain;

import java.util.Objects;

/**
 * A platform account the user authorized the application for.
 *
 * @param accountId the platform's id of the user, which tells the user's own messages from the other party's
 * @param name the account's display name on the platform
 */
public record ConnectedAccount(String accountId, String name, OAuthTokens tokens) {

    public ConnectedAccount {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(tokens, "tokens");
    }
}
