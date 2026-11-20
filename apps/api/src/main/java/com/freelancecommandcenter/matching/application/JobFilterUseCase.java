package com.freelancecommandcenter.matching.application;

import com.freelancecommandcenter.matching.application.port.out.JobFilterRepository;
import com.freelancecommandcenter.matching.domain.JobFilter;
import com.freelancecommandcenter.matching.domain.Keyword;
import com.freelancecommandcenter.shared.id.WorkspaceId;
import com.freelancecommandcenter.shared.platform.PlatformCode;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Reads and changes the job filter of a workspace. */
@Service
public class JobFilterUseCase {

    private final JobFilterRepository filters;

    JobFilterUseCase(JobFilterRepository filters) {
        this.filters = filters;
    }

    @Transactional(readOnly = true)
    public JobFilter get(WorkspaceId workspaceId) {
        return filters.find(workspaceId).orElseGet(() -> JobFilter.defaultFor(workspaceId));
    }

    @Transactional
    public JobFilter change(WorkspaceId workspaceId, List<String> keywords, List<PlatformCode> platforms) {
        JobFilter filter = get(workspaceId);
        filter.change(keywords.stream().map(Keyword::of).toList(), platforms);
        filters.save(filter);
        return filter;
    }
}
