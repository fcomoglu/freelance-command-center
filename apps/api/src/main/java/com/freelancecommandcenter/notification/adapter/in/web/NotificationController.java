package com.freelancecommandcenter.notification.adapter.in.web;

import com.freelancecommandcenter.notification.application.NotificationFeedUseCase;
import com.freelancecommandcenter.notification.domain.Notification;
import com.freelancecommandcenter.shared.pagination.KeysetCursor;
import com.freelancecommandcenter.shared.platform.PlatformCode;
import com.freelancecommandcenter.shared.security.AuthenticatedUser;
import com.freelancecommandcenter.shared.web.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Notifications")
@RestController
class NotificationController {

    private final NotificationFeedUseCase feed;

    NotificationController(NotificationFeedUseCase feed) {
        this.feed = feed;
    }

    @Operation(
            summary = "The workspace's platform notifications, newest first",
            security = @SecurityRequirement(name = "bearer"))
    @GetMapping("/api/v1/notifications")
    PageResponse<NotificationResponse> notifications(
            AuthenticatedUser caller,
            @Parameter(description = "`nextCursor` of the previous page") @RequestParam Optional<String> cursor,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit) {
        return PageResponse.from(
                feed.list(caller.workspaceId(), cursor.map(KeysetCursor::decode), limit),
                NotificationController::toResponse);
    }

    @Operation(
            summary = "Mark a notification as read",
            description = "Read state is kept by the application; the platform is not told.",
            security = @SecurityRequirement(name = "bearer"))
    @PostMapping("/api/v1/notifications/{notificationId}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void markRead(AuthenticatedUser caller, @PathVariable UUID notificationId) {
        feed.markRead(caller.workspaceId(), notificationId);
    }

    @Operation(summary = "Mark every notification as read", security = @SecurityRequirement(name = "bearer"))
    @PostMapping("/api/v1/notifications/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void markAllRead(AuthenticatedUser caller) {
        feed.markAllRead(caller.workspaceId());
    }

    private static NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(
                notification.id().toString(),
                notification.platform(),
                notification.kind().name().toLowerCase(Locale.ROOT),
                notification.title(),
                notification.body(),
                notification.url().toString(),
                notification.occurredAt(),
                notification.readAt().isPresent());
    }

    @Schema(
            name = "Notification",
            requiredProperties = {"id", "platform", "kind", "title", "body", "url", "occurredAt", "read"})
    record NotificationResponse(
            String id,
            PlatformCode platform,

            @Schema(
                    description = "What happened: a decision on one of the user's proposals, or a `notice` that"
                            + " carries the platform's own text",
                    allowableValues = {
                        "proposal_shortlisted",
                        "proposal_awarded",
                        "proposal_rejected",
                        "proposal_revoked",
                        "notice"
                    })
            String kind,

            @Schema(description = "The job of a proposal, or the subject of a notice; may be empty for a notice")
            String title,

            @Schema(description = "Plain text; usually empty for a proposal")
            String body,

            @Schema(description = "Where the user handles the notification on its platform")
            String url,

            Instant occurredAt,

            boolean read) {}
}
