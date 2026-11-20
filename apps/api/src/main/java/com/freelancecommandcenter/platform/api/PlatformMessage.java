package com.freelancecommandcenter.platform.api;

import com.freelancecommandcenter.shared.platform.PlatformCode;
import java.net.URI;
import java.time.Instant;
import java.util.Objects;

/**
 * A message in the platform-neutral form that every platform adapter and mail parser translates into (ADR 0013).
 * {@code platform} and {@code conversationId} identify the conversation on its platform, {@code externalId} the message
 * within it.
 *
 * @param counterpart the display name of the other party
 * @param body plain text; for a {@link Detail#NOTICE} whatever the notification shows, possibly empty
 * @param url the conversation on its platform
 */
public record PlatformMessage(
        PlatformCode platform,
        String conversationId,
        String externalId,
        String counterpart,
        Direction direction,
        Detail detail,
        String body,
        URI url,
        Instant sentAt) {

    public PlatformMessage {
        Objects.requireNonNull(platform, "platform");
        Objects.requireNonNull(conversationId, "conversationId");
        Objects.requireNonNull(externalId, "externalId");
        Objects.requireNonNull(counterpart, "counterpart");
        Objects.requireNonNull(direction, "direction");
        Objects.requireNonNull(detail, "detail");
        Objects.requireNonNull(body, "body");
        Objects.requireNonNull(url, "url");
        Objects.requireNonNull(sentAt, "sentAt");
    }

    public enum Direction {
        /** From the other party. */
        INBOUND,
        /** From the user, written on the platform. */
        OUTBOUND
    }

    public enum Detail {
        /** Read from a platform API: the body is the whole message. */
        FULL,
        /** Read from a notification e-mail: the body is what the e-mail shows. */
        NOTICE
    }
}
