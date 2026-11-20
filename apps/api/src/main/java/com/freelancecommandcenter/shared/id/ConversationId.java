package com.freelancecommandcenter.shared.id;

import java.util.Objects;
import java.util.UUID;

/** Identifies a conversation in the inbox of the {@code messaging} module. */
public record ConversationId(UUID value) {

    public ConversationId {
        Objects.requireNonNull(value, "value");
    }

    public static ConversationId newId() {
        return new ConversationId(Uuids.v7());
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
