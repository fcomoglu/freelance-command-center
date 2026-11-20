package com.freelancecommandcenter.matching.adapter.in.web;

import com.freelancecommandcenter.matching.application.JobFilterUseCase;
import com.freelancecommandcenter.matching.domain.JobFilter;
import com.freelancecommandcenter.matching.domain.Keyword;
import com.freelancecommandcenter.shared.platform.PlatformCode;
import com.freelancecommandcenter.shared.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Job filter")
@RestController
class JobFilterController {

    private final JobFilterUseCase filters;

    JobFilterController(JobFilterUseCase filters) {
        this.filters = filters;
    }

    @Operation(
            summary = "The workspace's job filter",
            description = "Decides which new jobs are notified. Without keywords no job is notified.",
            security = @SecurityRequirement(name = "bearer"))
    @GetMapping("/api/v1/job-filter")
    JobFilterResponse get(AuthenticatedUser caller) {
        return toResponse(filters.get(caller.workspaceId()));
    }

    @Operation(summary = "Replace the workspace's job filter", security = @SecurityRequirement(name = "bearer"))
    @PutMapping("/api/v1/job-filter")
    JobFilterResponse change(AuthenticatedUser caller, @Valid @RequestBody JobFilterRequest request) {
        return toResponse(filters.change(caller.workspaceId(), request.keywords(), request.platforms()));
    }

    private static JobFilterResponse toResponse(JobFilter filter) {
        return new JobFilterResponse(
                filter.keywords().stream().map(Keyword::value).toList(), filter.platforms());
    }

    @Schema(
            name = "JobFilterRequest",
            requiredProperties = {"keywords", "platforms"})
    record JobFilterRequest(
            @Schema(description = "Whole words or phrases, matched case-insensitively; duplicates are dropped")
            @NotNull
            @Size(max = JobFilter.MAX_KEYWORDS)
            List<@NotBlank @Size(max = Keyword.MAX_LENGTH) String> keywords,

            @NotNull List<@NotNull PlatformCode> platforms) {}

    @Schema(
            name = "JobFilter",
            requiredProperties = {"keywords", "platforms"})
    record JobFilterResponse(
            @Schema(description = "Lower-case, in the order given")
            List<String> keywords,

            List<PlatformCode> platforms) {}
}
