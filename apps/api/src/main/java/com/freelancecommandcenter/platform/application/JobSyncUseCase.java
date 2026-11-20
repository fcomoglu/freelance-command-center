package com.freelancecommandcenter.platform.application;

import com.freelancecommandcenter.platform.api.JobDiscovered;
import com.freelancecommandcenter.platform.application.port.out.JobSource;
import com.freelancecommandcenter.platform.application.port.out.JobSource.FetchedListing;
import com.freelancecommandcenter.platform.application.port.out.JobSyncStore;
import com.freelancecommandcenter.platform.application.port.out.PlatformUnavailableException;
import com.freelancecommandcenter.platform.domain.JobSyncState;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Reads new and changed listings from every platform with a job API and publishes them as {@link JobDiscovered}
 * events. A failed sync leaves the watermark unchanged, so the next run reads the same period again.
 */
@Service
public class JobSyncUseCase {

    private static final Logger log = LoggerFactory.getLogger(JobSyncUseCase.class);

    private final PlatformRegistry platforms;
    private final JobSyncStore store;
    private final ApplicationEventPublisher events;
    private final TransactionTemplate transaction;
    private final PlatformProperties properties;
    private final Clock clock;

    JobSyncUseCase(
            PlatformRegistry platforms,
            JobSyncStore store,
            ApplicationEventPublisher events,
            TransactionTemplate transaction,
            PlatformProperties properties,
            Clock clock) {
        this.platforms = platforms;
        this.store = store;
        this.events = events;
        this.transaction = transaction;
        this.properties = properties;
        this.clock = clock;
    }

    /** Syncs every platform; one unavailable platform does not stop the others. */
    public void syncAll() {
        for (JobSource source : platforms.jobSources()) {
            try {
                int count = sync(source);
                log.info("Job sync of {} read {} listings", source.platform().value(), count);
            } catch (PlatformUnavailableException exception) {
                log.warn("Job sync skipped: {}", exception.getMessage());
            }
        }
    }

    /**
     * Fetches outside the transaction so no database connection is held during the platform call; the raw payloads,
     * the events (outbox) and the new watermark are then stored together.
     *
     * @return the number of distinct listings read
     */
    int sync(JobSource source) {
        Instant now = clock.instant();
        JobSyncState state = store.findState(source.platform()).orElseGet(() -> JobSyncState.start(source.platform()));
        List<FetchedListing> fetched = latestPerListing(source.fetchUpdatedSince(
                state.fetchFrom(now, properties.jobSync().initialLookback())));

        transaction.executeWithoutResult(status -> {
            store.saveRawListings(fetched, now);
            fetched.forEach(item -> events.publishEvent(new JobDiscovered(item.listing())));
            state.recordSync(
                    fetched.stream().map(item -> item.listing().updatedAt()).max(Comparator.naturalOrder()), now);
            store.saveState(state);
        });
        return fetched.size();
    }

    /** Paged results can repeat a listing that changed during the sync; only its newest version is kept. */
    private static List<FetchedListing> latestPerListing(List<FetchedListing> fetched) {
        Map<String, FetchedListing> latest = new LinkedHashMap<>();
        for (FetchedListing item : fetched) {
            latest.merge(
                    item.listing().externalId(),
                    item,
                    (kept, candidate) -> candidate
                                    .listing()
                                    .updatedAt()
                                    .isAfter(kept.listing().updatedAt())
                            ? candidate
                            : kept);
        }
        return new ArrayList<>(latest.values());
    }
}
