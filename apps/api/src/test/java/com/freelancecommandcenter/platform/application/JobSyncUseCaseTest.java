package com.freelancecommandcenter.platform.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.freelancecommandcenter.platform.api.JobDiscovered;
import com.freelancecommandcenter.platform.api.JobListing;
import com.freelancecommandcenter.platform.application.port.out.JobSource;
import com.freelancecommandcenter.platform.application.port.out.JobSyncStore;
import com.freelancecommandcenter.platform.application.port.out.PlatformUnavailableException;
import com.freelancecommandcenter.platform.domain.JobSyncState;
import com.freelancecommandcenter.shared.platform.PlatformCode;
import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.unit.DataSize;

class JobSyncUseCaseTest {

    private static final Instant NOW = Instant.parse("2026-11-20T12:00:00Z");

    private final InMemoryStore store = new InMemoryStore();
    private final List<Object> events = new ArrayList<>();
    private final StubSource source = new StubSource();
    private final JobSyncUseCase jobSync = new JobSyncUseCase(
            new PlatformRegistry(List.of(source), List.of(), List.of(), List.of()),
            store,
            events::add,
            new TransactionTemplate(new NoTransactionManager()),
            new PlatformProperties(
                    new PlatformProperties.JobSync(true, Duration.ofMinutes(5), Duration.ZERO, Duration.ofHours(1)),
                    new PlatformProperties.Mail("inbound.localhost", "", DataSize.ofMegabytes(5)),
                    TestProperties.connections()),
            Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void publishesTheNewestVersionOfEachListingAndAdvancesTheWatermark() {
        source.result = List.of(
                fetched("1", NOW.minusSeconds(300), "v1"),
                fetched("2", NOW.minusSeconds(200), "v1"),
                fetched("1", NOW.minusSeconds(100), "v2"));

        jobSync.syncAll();

        assertThat(source.requestedSince).isEqualTo(NOW.minus(Duration.ofHours(1)));
        assertThat(events)
                .extracting(event -> ((JobDiscovered) event).listing().title())
                .containsExactly("1 v2", "2 v1");
        assertThat(store.raw).hasSize(2);
        assertThat(store.states.get(PlatformCode.FREELANCER).watermark()).contains(NOW.minusSeconds(100));
    }

    @Test
    void anUnavailablePlatformKeepsItsWatermarkSoTheNextSyncReadsTheSamePeriod() {
        store.saveState(JobSyncState.restore(PlatformCode.FREELANCER, NOW.minusSeconds(600), NOW.minusSeconds(300)));
        source.failure = new PlatformUnavailableException(PlatformCode.FREELANCER, "rate limited (HTTP 429)", null);

        jobSync.syncAll();

        assertThat(events).isEmpty();
        assertThat(store.states.get(PlatformCode.FREELANCER).watermark()).contains(NOW.minusSeconds(600));
    }

    private static JobSource.FetchedListing fetched(String id, Instant updatedAt, String version) {
        JobListing listing = new JobListing(
                PlatformCode.FREELANCER,
                id,
                id + " " + version,
                "",
                URI.create("https://www.freelancer.com/projects/" + id),
                null,
                List.of(),
                null,
                null,
                updatedAt,
                updatedAt);
        return new JobSource.FetchedListing(listing, "{}");
    }

    private static final class StubSource implements JobSource {

        private List<FetchedListing> result = List.of();
        private PlatformUnavailableException failure;
        private Instant requestedSince;

        @Override
        public PlatformCode platform() {
            return PlatformCode.FREELANCER;
        }

        @Override
        public List<FetchedListing> fetchUpdatedSince(Instant since) {
            requestedSince = since;
            if (failure != null) {
                throw failure;
            }
            return result;
        }
    }

    private static final class InMemoryStore implements JobSyncStore {

        private final Map<PlatformCode, JobSyncState> states = new EnumMap<>(PlatformCode.class);
        private final List<JobSource.FetchedListing> raw = new ArrayList<>();

        @Override
        public Optional<JobSyncState> findState(PlatformCode platform) {
            return Optional.ofNullable(states.get(platform));
        }

        @Override
        public void saveState(JobSyncState state) {
            states.put(state.platform(), state);
        }

        @Override
        public void saveRawListings(List<JobSource.FetchedListing> listings, Instant fetchedAt) {
            raw.addAll(listings);
        }
    }

    /** Runs the callback without a real transaction. */
    private static final class NoTransactionManager extends AbstractPlatformTransactionManager {

        private static final long serialVersionUID = 1L;

        @Override
        protected Object doGetTransaction() {
            return new Object();
        }

        @Override
        protected void doBegin(Object transaction, TransactionDefinition definition) {}

        @Override
        protected void doCommit(DefaultTransactionStatus status) {}

        @Override
        protected void doRollback(DefaultTransactionStatus status) {}
    }
}
