package com.freelancecommandcenter.job.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.freelancecommandcenter.shared.money.Money;
import com.freelancecommandcenter.shared.platform.PlatformCode;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class JobTest {

    private static final Instant POSTED = Instant.parse("2026-11-20T10:00:00Z");

    @Test
    void onlyANewerPlatformUpdateReplacesTheContent() {
        Job job = Job.discover(PlatformCode.FREELANCER, "42", content("First"), POSTED, POSTED, POSTED);

        assertThat(job.update(content("Repeated"), POSTED)).isFalse();
        assertThat(job.update(content("Older"), POSTED.minusSeconds(60))).isFalse();
        assertThat(job.content().title()).isEqualTo("First");

        assertThat(job.update(content("Edited"), POSTED.plusSeconds(60))).isTrue();
        assertThat(job.content().title()).isEqualTo("Edited");
        assertThat(job.sourceUpdatedAt()).isEqualTo(POSTED.plusSeconds(60));
        assertThat(job.postedAt()).isEqualTo(POSTED);
    }

    @Test
    void aJobNeedsATitle() {
        assertThatThrownBy(() -> content(" ")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aBudgetRangeCannotEndBelowItsStart() {
        assertThatThrownBy(() -> new JobBudget(
                        JobBudget.Type.FIXED, Money.of("500", "USD"), Optional.of(Money.of("100", "USD"))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static JobContent content(String title) {
        return new JobContent(
                title,
                "",
                URI.create("https://www.freelancer.com/projects/42"),
                Optional.empty(),
                List.of(),
                Optional.empty(),
                Optional.empty());
    }
}
