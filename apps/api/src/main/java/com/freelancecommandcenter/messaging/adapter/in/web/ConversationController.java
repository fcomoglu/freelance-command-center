package com.freelancecommandcenter.messaging.adapter.in.web;

import com.freelancecommandcenter.messaging.application.ConversationQuery;
import com.freelancecommandcenter.messaging.application.MarkConversationReadUseCase;
import com.freelancecommandcenter.messaging.domain.Conversation;
import com.freelancecommandcenter.messaging.domain.Message;
import com.freelancecommandcenter.shared.id.ConversationId;
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

@Tag(name = "Messages")
@RestController
class ConversationController {

    private final ConversationQuery query;
    private final MarkConversationReadUseCase reading;

    ConversationController(ConversationQuery query, MarkConversationReadUseCase reading) {
        this.query = query;
        this.reading = reading;
    }

    @Operation(
            summary = "The workspace's conversations, newest message first",
            security = @SecurityRequirement(name = "bearer"))
    @GetMapping("/api/v1/conversations")
    PageResponse<ConversationResponse> conversations(
            AuthenticatedUser caller,
            @Parameter(description = "`nextCursor` of the previous page") @RequestParam Optional<String> cursor,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit) {
        return PageResponse.from(
                query.list(caller.workspaceId(), cursor.map(KeysetCursor::decode), limit),
                ConversationController::toResponse);
    }

    @Operation(
            summary = "The messages of a conversation, newest first",
            security = @SecurityRequirement(name = "bearer"))
    @GetMapping("/api/v1/conversations/{conversationId}/messages")
    PageResponse<MessageResponse> messages(
            AuthenticatedUser caller,
            @PathVariable UUID conversationId,
            @Parameter(description = "`nextCursor` of the previous page") @RequestParam Optional<String> cursor,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit) {
        return PageResponse.from(
                query.messages(
                        caller.workspaceId(),
                        new ConversationId(conversationId),
                        cursor.map(KeysetCursor::decode),
                        limit),
                ConversationController::toResponse);
    }

    @Operation(
            summary = "Mark a conversation as read",
            description = "Read state is kept by the application; the platform is not told.",
            security = @SecurityRequirement(name = "bearer"))
    @PostMapping("/api/v1/conversations/{conversationId}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void markRead(AuthenticatedUser caller, @PathVariable UUID conversationId) {
        reading.markRead(caller.workspaceId(), new ConversationId(conversationId));
    }

    private static ConversationResponse toResponse(Conversation conversation) {
        return new ConversationResponse(
                conversation.id().toString(),
                conversation.platform(),
                conversation.counterpart(),
                conversation.url().toString(),
                lowerCase(conversation.detail()),
                conversation.lastMessageAt(),
                conversation.preview(),
                conversation.unreadCount());
    }

    private static MessageResponse toResponse(Message message) {
        return new MessageResponse(
                message.id().toString(),
                lowerCase(message.direction()),
                lowerCase(message.detail()),
                message.body(),
                message.sentAt());
    }

    private static String lowerCase(Enum<?> value) {
        return value.name().toLowerCase(Locale.ROOT);
    }

    @Schema(
            name = "Conversation",
            requiredProperties = {
                "id",
                "platform",
                "counterpart",
                "url",
                "detail",
                "lastMessageAt",
                "preview",
                "unreadCount"
            })
    record ConversationResponse(
            String id,
            PlatformCode platform,

            @Schema(description = "The other party's name") String counterpart,

            @Schema(description = "The conversation on its platform; replies are written there")
            String url,

            @Schema(
                    description = "Detail of the newest message: `full` from a platform API, `notice` from a"
                            + " notification e-mail",
                    allowableValues = {"full", "notice"})
            String detail,

            Instant lastMessageAt,

            @Schema(description = "The start of the newest message on one line; empty when it has no text")
            String preview,

            int unreadCount) {}

    @Schema(
            name = "Message",
            requiredProperties = {"id", "direction", "detail", "body", "sentAt"})
    record MessageResponse(
            String id,

            @Schema(allowableValues = {"inbound", "outbound"})
            String direction,

            @Schema(
                    description = "`notice`: what a notification e-mail showed of the message",
                    allowableValues = {"full", "notice"})
            String detail,

            @Schema(description = "Plain text; may be empty for a notice")
            String body,

            Instant sentAt) {}
}
