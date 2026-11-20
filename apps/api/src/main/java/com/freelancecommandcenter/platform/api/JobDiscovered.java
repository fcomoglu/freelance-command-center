package com.freelancecommandcenter.platform.api;

import com.freelancecommandcenter.shared.id.WorkspaceId;
import java.util.Objects;

/**
 * Published for every listing a platform sync or a forwarded e-mail delivers, new or changed. The same listing can be
 * published again, so handlers must be idempotent.
 *
 * @param owner the only workspace that may see the listing: the one that forwarded the e-mail it was read from;
 *     {@code null} for a listing read from a platform API, which every workspace sees
 */
public record JobDiscovered(JobListing listing, WorkspaceId owner) {

    public JobDiscovered {
        Objects.requireNonNull(listing, "listing");
    }

    /** A listing read from a platform API. */
    public JobDiscovered(JobListing listing) {
        this(listing, null);
    }
}
