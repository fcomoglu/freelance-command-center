package com.freelancecommandcenter.platform.api;

import com.freelancecommandcenter.shared.platform.PlatformCode;
import java.net.URI;
import java.time.Instant;
import java.util.Objects;

/**
 * A platform notification in the platform-neutral form (ADR 0015): a change of one of the user's proposals, read from a
 * platform API, or a notice read from a forwarded e-mail. {@code platform} and {@code externalId} identify it, so the
 * same notification read twice is recognized.
 *
 * @param title the job of a proposal, or the subject of a notice
 * @param body plain text; for a proposal change usually empty, for a notice what the e-mail shows, possibly empty
 * @param url where the user handles it on the platform
 */
public record PlatformNotification(
        PlatformCode platform, String externalId, Kind kind, String title, String body, URI url, Instant occurredAt) {

    public PlatformNotification {
        Objects.requireNonNull(platform, "platform");
        Objects.requireNonNull(externalId, "externalId");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(body, "body");
        Objects.requireNonNull(url, "url");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }

    /** The proposal kinds are named by what happened; clients translate them, so the API stays language-neutral. */
    public enum Kind {
        PROPOSAL_SHORTLISTED,
        PROPOSAL_AWARDED,
        PROPOSAL_REJECTED,
        PROPOSAL_REVOKED,
        /** Anything else the platform announces; the title and body carry the platform's own text. */
        NOTICE
    }
}
