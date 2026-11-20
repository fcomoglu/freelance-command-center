package com.freelancecommandcenter.notification.domain;

import com.freelancecommandcenter.platform.api.PlatformNotification;
import com.freelancecommandcenter.shared.id.WorkspaceId;
import com.freelancecommandcenter.shared.platform.PlatformCode;
import java.net.URI;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * An entry of a workspace's notification feed (ADR 0015). It is unread until the user marks it read in a client; the
 * platform's own read state is not used.
 *
 * @param externalId identifies the notification on its platform, so a repeated delivery is recognized
 * @param url where the user handles it on the platform
 */
public record Notification(
        UUID id,
        WorkspaceId workspaceId,
        PlatformCode platform,
        String externalId,
        PlatformNotification.Kind kind,
        String title,
        String body,
        URI url,
        Instant occurredAt,
        Optional<Instant> readAt) {

    public Notification {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(workspaceId, "workspaceId");
        Objects.requireNonNull(platform, "platform");
        Objects.requireNonNull(externalId, "externalId");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(body, "body");
        Objects.requireNonNull(url, "url");
        Objects.requireNonNull(occurredAt, "occurredAt");
        Objects.requireNonNull(readAt, "readAt");
    }

    public static Notification unread(UUID id, WorkspaceId workspaceId, PlatformNotification source) {
        return new Notification(
                id,
                workspaceId,
                source.platform(),
                source.externalId(),
                source.kind(),
                source.title(),
                source.body(),
                source.url(),
                source.occurredAt(),
                Optional.empty());
    }
}
