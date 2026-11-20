package com.freelancecommandcenter.job.domain;

import com.freelancecommandcenter.shared.id.JobId;
import com.freelancecommandcenter.shared.platform.PlatformCode;
import java.time.Instant;
import java.util.Objects;

/**
 * A job in the catalog, unique per platform listing ({@code platform} + {@code externalId}). Its content follows the
 * platform: only a newer platform update replaces it, so repeated or out-of-order deliveries change nothing.
 */
public final class Job {

    private final JobId id;
    private final PlatformCode platform;
    private final String externalId;
    private JobContent content;
    private final Instant postedAt;
    private Instant sourceUpdatedAt;
    private final Instant discoveredAt;

    private Job(
            JobId id,
            PlatformCode platform,
            String externalId,
            JobContent content,
            Instant postedAt,
            Instant sourceUpdatedAt,
            Instant discoveredAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.platform = Objects.requireNonNull(platform, "platform");
        this.externalId = Objects.requireNonNull(externalId, "externalId");
        this.content = Objects.requireNonNull(content, "content");
        this.postedAt = Objects.requireNonNull(postedAt, "postedAt");
        this.sourceUpdatedAt = Objects.requireNonNull(sourceUpdatedAt, "sourceUpdatedAt");
        this.discoveredAt = Objects.requireNonNull(discoveredAt, "discoveredAt");
    }

    public static Job discover(
            PlatformCode platform,
            String externalId,
            JobContent content,
            Instant postedAt,
            Instant sourceUpdatedAt,
            Instant now) {
        return new Job(JobId.newId(), platform, externalId, content, postedAt, sourceUpdatedAt, now);
    }

    /** Rebuilds a job from storage. */
    public static Job restore(
            JobId id,
            PlatformCode platform,
            String externalId,
            JobContent content,
            Instant postedAt,
            Instant sourceUpdatedAt,
            Instant discoveredAt) {
        return new Job(id, platform, externalId, content, postedAt, sourceUpdatedAt, discoveredAt);
    }

    /**
     * Applies content the platform changed at {@code updatedAt}.
     *
     * @return whether the job changed; {@code false} for an update that is not newer than the current content
     */
    public boolean update(JobContent newContent, Instant updatedAt) {
        Objects.requireNonNull(newContent, "newContent");
        if (!updatedAt.isAfter(sourceUpdatedAt)) {
            return false;
        }
        content = newContent;
        sourceUpdatedAt = updatedAt;
        return true;
    }

    public JobId id() {
        return id;
    }

    public PlatformCode platform() {
        return platform;
    }

    public String externalId() {
        return externalId;
    }

    public JobContent content() {
        return content;
    }

    public Instant postedAt() {
        return postedAt;
    }

    public Instant sourceUpdatedAt() {
        return sourceUpdatedAt;
    }

    public Instant discoveredAt() {
        return discoveredAt;
    }
}
