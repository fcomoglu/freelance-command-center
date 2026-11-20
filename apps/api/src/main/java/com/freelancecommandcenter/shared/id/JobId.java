package com.freelancecommandcenter.shared.id;

import java.util.Objects;
import java.util.UUID;

/** Identifies a job in the catalog of the {@code job} module. */
public record JobId(UUID value) {

    public JobId {
        Objects.requireNonNull(value, "value");
    }

    public static JobId newId() {
        return new JobId(Uuids.v7());
    }

    public static JobId of(String value) {
        return new JobId(UUID.fromString(value));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
