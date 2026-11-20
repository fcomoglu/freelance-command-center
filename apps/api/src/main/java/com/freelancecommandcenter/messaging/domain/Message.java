package com.freelancecommandcenter.messaging.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A message of a conversation, unique per conversation by the platform's message id.
 *
 * @param body plain text; for a {@link Detail#NOTICE} whatever the notification showed, possibly empty
 */
public record Message(UUID id, String externalId, Direction direction, Detail detail, String body, Instant sentAt) {

    public Message {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(externalId, "externalId");
        Objects.requireNonNull(direction, "direction");
        Objects.requireNonNull(detail, "detail");
        Objects.requireNonNull(body, "body");
        Objects.requireNonNull(sentAt, "sentAt");
    }

    /** Long enough for a list row or a notification; the full text stays in the body. */
    static final int PREVIEW_LENGTH = 160;

    /** The start of the body on one line; empty when the body has no text. */
    public String preview() {
        String text = body.strip().replaceAll("\\s+", " ");
        return text.length() <= PREVIEW_LENGTH
                ? text
                : text.substring(0, PREVIEW_LENGTH - 1).stripTrailing() + "…";
    }

    public enum Direction {
        INBOUND,
        OUTBOUND
    }

    /** How much of the message the inbox has (ADR 0013). */
    public enum Detail {
        /** The whole message, read from a platform API. */
        FULL,
        /** What a notification e-mail showed of it. */
        NOTICE
    }
}
