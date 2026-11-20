package com.freelancecommandcenter.platform.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.freelancecommandcenter.shared.platform.PlatformCode;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class JobSyncStateTest {

    private static final Instant NOW = Instant.parse("2026-11-20T12:00:00Z");

    @Test
    void theFirstSyncLooksBackOnlyTheInitialPeriod() {
        JobSyncState state = JobSyncState.start(PlatformCode.FREELANCER);

        assertThat(state.fetchFrom(NOW, Duration.ofHours(1))).isEqualTo(NOW.minus(Duration.ofHours(1)));
    }

    @Test
    void laterSyncsContinueFromTheNewestUpdateSeen() {
        JobSyncState state = JobSyncState.start(PlatformCode.FREELANCER);
        Instant newest = NOW.minusSeconds(30);

        state.recordSync(Optional.of(newest), NOW);

        assertThat(state.fetchFrom(NOW.plusSeconds(300), Duration.ofHours(1))).isEqualTo(newest);
        assertThat(state.lastSyncedAt()).contains(NOW);
    }

    @Test
    void theWatermarkNeverMovesBack() {
        JobSyncState state = JobSyncState.restore(PlatformCode.FREELANCER, NOW, NOW);

        state.recordSync(Optional.of(NOW.minusSeconds(600)), NOW.plusSeconds(300));
        state.recordSync(Optional.empty(), NOW.plusSeconds(600));

        assertThat(state.watermark()).contains(NOW);
        assertThat(state.lastSyncedAt()).contains(NOW.plusSeconds(600));
    }
}
