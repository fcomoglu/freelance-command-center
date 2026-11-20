package com.freelancecommandcenter.matching.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.freelancecommandcenter.job.api.JobPublished;
import com.freelancecommandcenter.matching.api.JobMatched;
import com.freelancecommandcenter.matching.application.port.out.JobFilterRepository;
import com.freelancecommandcenter.matching.domain.JobFilter;
import com.freelancecommandcenter.matching.domain.Keyword;
import com.freelancecommandcenter.shared.id.JobId;
import com.freelancecommandcenter.shared.id.WorkspaceId;
import com.freelancecommandcenter.shared.platform.PlatformCode;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class MatchJobUseCaseTest {

    private final WorkspaceId owner = WorkspaceId.newId();
    private final WorkspaceId other = WorkspaceId.newId();
    private final List<Object> events = new ArrayList<>();
    private final MatchJobUseCase matching =
            new MatchJobUseCase(new Filters(List.of(filter(owner), filter(other))), events::add);

    @Test
    void aPublicJobIsMatchedAgainstEveryWorkspace() {
        assertThat(matching.match(job(null))).isEqualTo(2);

        assertThat(events).map(event -> ((JobMatched) event).workspaceId()).containsExactlyInAnyOrder(owner, other);
    }

    @Test
    void aPrivateJobIsMatchedOnlyAgainstItsOwner() {
        assertThat(matching.match(job(owner))).isEqualTo(1);

        assertThat(events).map(event -> ((JobMatched) event).workspaceId()).containsExactly(owner);
    }

    private static JobFilter filter(WorkspaceId workspace) {
        return JobFilter.restore(workspace, List.of(Keyword.of("spring")), List.of(PlatformCode.ARC_DEV));
    }

    private static JobPublished job(WorkspaceId owner) {
        return new JobPublished(
                JobId.newId(),
                owner,
                PlatformCode.ARC_DEV,
                "Spring Boot developer",
                "",
                List.of(),
                URI.create("https://arc.dev/remote-jobs/1"),
                Instant.parse("2026-11-20T12:00:00Z"));
    }

    private record Filters(List<JobFilter> all) implements JobFilterRepository {

        @Override
        public Optional<JobFilter> find(WorkspaceId workspaceId) {
            return all.stream()
                    .filter(filter -> filter.workspaceId().equals(workspaceId))
                    .findFirst();
        }

        @Override
        public void save(JobFilter filter) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<JobFilter> findActiveFor(PlatformCode platform) {
            return all;
        }
    }
}
