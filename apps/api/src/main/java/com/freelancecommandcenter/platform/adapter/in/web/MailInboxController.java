package com.freelancecommandcenter.platform.adapter.in.web;

import com.freelancecommandcenter.platform.application.InboundMailQuery;
import com.freelancecommandcenter.platform.application.MailInboxUseCase;
import com.freelancecommandcenter.platform.domain.InboundMail;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Mail inbox")
@RestController
class MailInboxController {

    private final MailInboxUseCase inbox;
    private final InboundMailQuery mails;

    MailInboxController(MailInboxUseCase inbox, InboundMailQuery mails) {
        this.inbox = inbox;
        this.mails = mails;
    }

    @Operation(
            summary = "The workspace's address for forwarded platform e-mails",
            description = "Fiverr, Bionluk and Arc.dev are read from their notification e-mails, forwarded here.",
            security = @SecurityRequirement(name = "bearer"))
    @GetMapping("/api/v1/mail-inbox")
    MailInboxResponse get(AuthenticatedUser caller) {
        return new MailInboxResponse(inbox.address(caller.workspaceId()));
    }

    @Operation(
            summary = "Replace the address",
            description = "Mail to the previous address is no longer accepted.",
            security = @SecurityRequirement(name = "bearer"))
    @PostMapping("/api/v1/mail-inbox/renewal")
    MailInboxResponse renew(AuthenticatedUser caller) {
        return new MailInboxResponse(inbox.renew(caller.workspaceId()));
    }

    @Operation(summary = "Mails the address received, newest first", security = @SecurityRequirement(name = "bearer"))
    @GetMapping("/api/v1/inbound-mails")
    PageResponse<InboundMailResponse> mails(
            AuthenticatedUser caller,
            @Parameter(description = "`nextCursor` of the previous page") @RequestParam Optional<String> cursor,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit) {
        return PageResponse.from(
                mails.list(caller.workspaceId(), cursor.map(KeysetCursor::decode), limit),
                MailInboxController::toResponse);
    }

    private static InboundMailResponse toResponse(InboundMail mail) {
        return new InboundMailResponse(
                mail.id().toString(),
                mail.platform().orElse(null),
                mail.sender(),
                mail.subject(),
                mail.receivedAt(),
                mail.status().name().toLowerCase(Locale.ROOT));
    }

    @Schema(
            name = "MailInbox",
            requiredProperties = {"address"})
    record MailInboxResponse(String address) {}

    @Schema(
            name = "InboundMail",
            requiredProperties = {"id", "sender", "subject", "receivedAt", "status"})
    record InboundMailResponse(
            String id,

            @Schema(description = "Absent for senders other than the supported platforms")
            PlatformCode platform,

            String sender,
            String subject,
            Instant receivedAt,

            @Schema(allowableValues = {"processed", "unrecognized", "unknown_sender"})
            String status) {}
}
