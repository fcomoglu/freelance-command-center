package com.freelancecommandcenter.platform.adapter.out.persistence;

import com.freelancecommandcenter.platform.application.port.out.JobSource.FetchedListing;
import com.freelancecommandcenter.platform.application.port.out.JobSyncStore;
import com.freelancecommandcenter.platform.domain.JobSyncState;
import com.freelancecommandcenter.shared.platform.PlatformCode;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/** Plain SQL: both tables are simple upserts keyed by platform and listing, which JPA would only complicate. */
@Component
class JobSyncPersistenceAdapter implements JobSyncStore {

    private final JdbcClient jdbc;
    private final JdbcTemplate batch;

    JobSyncPersistenceAdapter(JdbcClient jdbc, JdbcTemplate batch) {
        this.jdbc = jdbc;
        this.batch = batch;
    }

    @Override
    public Optional<JobSyncState> findState(PlatformCode platform) {
        return jdbc.sql("SELECT watermark, last_synced_at FROM platform.job_sync_states WHERE platform = ?")
                .param(platform.value())
                .query((row, number) -> JobSyncState.restore(
                        platform, instant(row.getTimestamp("watermark")), instant(row.getTimestamp("last_synced_at"))))
                .optional();
    }

    @Override
    public void saveState(JobSyncState state) {
        jdbc.sql("""
                        INSERT INTO platform.job_sync_states (platform, watermark, last_synced_at)
                        VALUES (?, ?, ?)
                        ON CONFLICT (platform)
                        DO UPDATE SET watermark = EXCLUDED.watermark, last_synced_at = EXCLUDED.last_synced_at
                        """)
                .params(
                        state.platform().value(),
                        state.watermark().map(Timestamp::from).orElse(null),
                        state.lastSyncedAt().map(Timestamp::from).orElse(null))
                .update();
    }

    @Override
    public void saveRawListings(List<FetchedListing> listings, Instant fetchedAt) {
        batch.batchUpdate(
                """
                INSERT INTO platform.raw_job_listings (platform, external_id, payload, fetched_at)
                VALUES (?, ?, CAST(? AS jsonb), ?)
                ON CONFLICT (platform, external_id)
                DO UPDATE SET payload = EXCLUDED.payload, fetched_at = EXCLUDED.fetched_at
                """,
                listings.stream()
                        .map(item -> new Object[] {
                            item.listing().platform().value(),
                            item.listing().externalId(),
                            item.rawJson(),
                            Timestamp.from(fetchedAt)
                        })
                        .toList());
    }

    private static Instant instant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }
}
