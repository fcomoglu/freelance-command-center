package com.freelancecommandcenter.shared.id;

import java.util.Objects;
import java.util.UUID;

public record WorkspaceId(UUID value) {

    public WorkspaceId {
        Objects.requireNonNull(value, "value");
    }

    public static WorkspaceId newId() {
        return new WorkspaceId(Uuids.v7());
    }

    public static WorkspaceId of(String value) {
        return new WorkspaceId(UUID.fromString(value));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
