package com.freelancecommandcenter.identity.domain;

import com.freelancecommandcenter.shared.id.UserId;
import com.freelancecommandcenter.shared.id.WorkspaceId;
import java.time.Instant;
import java.util.Objects;

/** Owner of all user data. Each user has one workspace; memberships for teams can be added later. */
public record Workspace(WorkspaceId id, UserId ownerId, Instant createdAt) {

    public Workspace {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(ownerId, "ownerId");
        Objects.requireNonNull(createdAt, "createdAt");
    }

    public static Workspace createFor(UserId ownerId, Instant now) {
        return new Workspace(WorkspaceId.newId(), ownerId, now);
    }
}
