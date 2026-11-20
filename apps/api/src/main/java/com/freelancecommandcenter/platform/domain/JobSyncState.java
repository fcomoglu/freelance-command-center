package com.freelancecommandcenter.platform.domain;

import com.freelancecommandcenter.shared.platform.PlatformCode;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * How far the job sync of one platform has read: the newest listing update seen so far (a high-watermark). Each sync
 * asks the platform only for listings updated since then.
 */
public final class JobSyncState {

    private final PlatformCode platform;
    private Instant watermark;
    private Instant lastSyncedAt;

    private JobSyncState(PlatformCode platform, Instant watermark, Instant lastSyncedAt) {
        this.platform = Objects.requireNonNull(platform, "platform");
        this.watermark = watermark;
        this.lastSyncedAt = lastSyncedAt;
    }

    public static JobSyncState start(PlatformCode platform) {
        return new JobSyncState(platform, null, null);
    }

    public static JobSyncState restore(PlatformCode platform, Instant watermark, Instant lastSyncedAt) {
        return new JobSyncState(platform, watermark, lastSyncedAt);
    }

    /** The first sync looks back {@code initialLookback} instead of reading the platform's whole history. */
    public Instant fetchFrom(Instant now, Duration initialLookback) {
        return watermark != null ? watermark : now.minus(initialLookback);
    }

    /** Never moves back, so a late or repeated batch cannot make the next sync read old listings again. */
    public void recordSync(Optional<Instant> newestUpdate, Instant now) {
        newestUpdate
                .filter(update -> watermark == null || update.isAfter(watermark))
                .ifPresent(update -> watermark = update);
        lastSyncedAt = now;
    }

    public PlatformCode platform() {
        return platform;
    }

    public Optional<Instant> watermark() {
        return Optional.ofNullable(watermark);
    }

    public Optional<Instant> lastSyncedAt() {
        return Optional.ofNullable(lastSyncedAt);
    }
}
