package com.freelancecommandcenter.shared.security;

import com.freelancecommandcenter.shared.id.UserId;
import com.freelancecommandcenter.shared.id.WorkspaceId;
import java.util.Objects;

/**
 * The caller of an authenticated request. Controllers declare it as a parameter; it is resolved from the access
 * token, whose subject is the user id and whose {@value #WORKSPACE_CLAIM} claim is the workspace id.
 */
public record AuthenticatedUser(UserId userId, WorkspaceId workspaceId) {

    public static final String WORKSPACE_CLAIM = "wid";

    public AuthenticatedUser {
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(workspaceId, "workspaceId");
    }
}
