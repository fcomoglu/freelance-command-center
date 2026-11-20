package com.freelancecommandcenter.job.adapter.in.web;

import com.freelancecommandcenter.job.application.JobFeedQuery;
import com.freelancecommandcenter.job.domain.Job;
import com.freelancecommandcenter.job.domain.JobBudget;
import com.freelancecommandcenter.job.domain.JobContent;
import com.freelancecommandcenter.shared.pagination.KeysetCursor;
import com.freelancecommandcenter.shared.platform.PlatformCode;
import com.freelancecommandcenter.shared.web.MoneyDto;
import com.freelancecommandcenter.shared.web.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Jobs")
@RestController
class JobController {

    private final JobFeedQuery feed;

    JobController(JobFeedQuery feed) {
        this.feed = feed;
    }

    @Operation(summary = "The job feed, newest posting first", security = @SecurityRequirement(name = "bearer"))
    @GetMapping("/api/v1/jobs")
    PageResponse<JobResponse> jobs(
            @Parameter(description = "Only jobs of this platform") @RequestParam Optional<PlatformCode> platform,
            @Parameter(description = "`nextCursor` of the previous page") @RequestParam Optional<String> cursor,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit) {
        return PageResponse.from(
                feed.list(platform, cursor.map(KeysetCursor::decode), limit), JobController::toResponse);
    }

    private static JobResponse toResponse(Job job) {
        JobContent content = job.content();
        return new JobResponse(
                job.id().toString(),
                job.platform(),
                content.title(),
                content.description(),
                content.url().toString(),
                content.budget().map(JobController::toResponse).orElse(null),
                content.skills(),
                content.bidCount().orElse(null),
                content.language().orElse(null),
                job.postedAt());
    }

    private static BudgetResponse toResponse(JobBudget budget) {
        return new BudgetResponse(
                budget.type().name().toLowerCase(Locale.ROOT),
                MoneyDto.from(budget.minimum()),
                budget.maximum().map(MoneyDto::from).orElse(null));
    }

    @Schema(
            name = "Job",
            requiredProperties = {"id", "platform", "title", "description", "url", "skills", "postedAt"})
    record JobResponse(
            String id,
            PlatformCode platform,
            String title,

            @Schema(description = "Plain text; empty when the platform gives none")
            String description,

            @Schema(description = "The listing on its platform")
            String url,

            BudgetResponse budget,
            List<String> skills,
            Integer bidCount,

            @Schema(description = "ISO 639-1 code of the listing's language")
            String language,

            Instant postedAt) {}

    @Schema(
            name = "JobBudget",
            requiredProperties = {"type", "minimum"})
    record BudgetResponse(
            @Schema(allowableValues = {"fixed", "hourly"}) String type,
            MoneyDto minimum,

            @Schema(description = "Absent for a single amount")
            MoneyDto maximum) {}
}
