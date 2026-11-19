package com.freelancecommandcenter.identity.application.port.out;

import com.freelancecommandcenter.shared.id.UserId;
import com.freelancecommandcenter.shared.id.WorkspaceId;
import java.time.Instant;

/** Issues short-lived, signed access tokens that every module accepts. */
public interface AccessTokenIssuer {

    AccessToken issue(UserId userId, WorkspaceId workspaceId, Instant now);

    record AccessToken(String value, Instant expiresAt) {}
}
