package com.freelancecommandcenter.platform.domain;

import com.freelancecommandcenter.shared.id.WorkspaceId;
import com.freelancecommandcenter.shared.platform.PlatformCode;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * An e-mail that arrived at a workspace's inbox. The raw message is stored separately, so a better parser can read it
 * again.
 *
 * @param messageId the Message-ID header, which makes a re-delivered mail recognizable
 * @param platform the platform of the sender; empty for mail from anyone else
 */
public record InboundMail(
        UUID id,
        WorkspaceId workspaceId,
        String messageId,
        Optional<PlatformCode> platform,
        String sender,
        String subject,
        Optional<Instant> sentAt,
        Instant receivedAt,
        Status status) {

    public InboundMail {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(workspaceId, "workspaceId");
        Objects.requireNonNull(messageId, "messageId");
        Objects.requireNonNull(platform, "platform");
        Objects.requireNonNull(sender, "sender");
        Objects.requireNonNull(subject, "subject");
        Objects.requireNonNull(sentAt, "sentAt");
        Objects.requireNonNull(receivedAt, "receivedAt");
        Objects.requireNonNull(status, "status");
    }

    public enum Status {
        /** The content was turned into jobs, messages or notifications. */
        PROCESSED,
        /** From a supported platform, but no parser understands this kind of mail yet; it is shown as a notice. */
        UNRECOGNIZED,
        /** Not from a platform read through e-mail. */
        UNKNOWN_SENDER
    }
}
