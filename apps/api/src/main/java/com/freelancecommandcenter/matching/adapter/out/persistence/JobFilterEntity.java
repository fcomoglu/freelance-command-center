package com.freelancecommandcenter.matching.adapter.out.persistence;

import com.freelancecommandcenter.matching.domain.JobFilter;
import com.freelancecommandcenter.matching.domain.Keyword;
import com.freelancecommandcenter.shared.id.WorkspaceId;
import com.freelancecommandcenter.shared.platform.PlatformCode;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Arrays;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(schema = "matching", name = "job_filters")
class JobFilterEntity {

    @Id
    private UUID workspaceId;

    @JdbcTypeCode(SqlTypes.ARRAY)
    private String[] keywords;

    @JdbcTypeCode(SqlTypes.ARRAY)
    private String[] platforms;

    protected JobFilterEntity() {}

    JobFilterEntity(UUID workspaceId) {
        this.workspaceId = workspaceId;
    }

    void apply(JobFilter filter) {
        keywords = filter.keywords().stream().map(Keyword::value).toArray(String[]::new);
        platforms = filter.platforms().stream().map(PlatformCode::value).toArray(String[]::new);
    }

    JobFilter toDomain() {
        return JobFilter.restore(
                new WorkspaceId(workspaceId),
                Arrays.stream(keywords).map(Keyword::of).toList(),
                Arrays.stream(platforms).map(PlatformCode::fromValue).toList());
    }
}
