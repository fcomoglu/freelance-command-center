package com.freelancecommandcenter.messaging.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.freelancecommandcenter.shared.id.WorkspaceId;
import com.freelancecommandcenter.shared.platform.PlatformCode;
import java.net.URI;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ConversationTest {

    private static final Instant START = Instant.parse("2026-11-20T10:00:00Z");
    private static final URI URL = URI.create("https://arc.dev/messages/1");

    private final Conversation conversation = Conversation.start(
            WorkspaceId.newId(), PlatformCode.ARC_DEV, "1", "Ada", URL, Message.Detail.NOTICE, START);

    @Test
    void inboundMessagesCountAsUnreadUntilTheConversationIsRead() {
        conversation.record(message(Message.Direction.INBOUND, "Hi", START), "Ada", URL);
        conversation.record(message(Message.Direction.OUTBOUND, "Hello", START.plusSeconds(60)), "Ada", URL);
        conversation.record(message(Message.Direction.INBOUND, "Are you free?", START.plusSeconds(120)), "Ada", URL);

        assertThat(conversation.unreadCount()).isEqualTo(2);
        conversation.markRead();
        assertThat(conversation.unreadCount()).isZero();
    }

    @Test
    void theSummaryFollowsTheNewestMessageEvenWhenAnOlderOneArrivesLate() {
        URI renamed = URI.create("https://arc.dev/messages/1?tab=chat");
        conversation.record(message(Message.Direction.INBOUND, "Newest", START.plusSeconds(60)), "Ada L.", renamed);
        conversation.record(message(Message.Direction.INBOUND, "Older", START), "Ada", URL);

        assertThat(conversation.preview()).isEqualTo("Newest");
        assertThat(conversation.counterpart()).isEqualTo("Ada L.");
        assertThat(conversation.url()).isEqualTo(renamed);
        assertThat(conversation.lastMessageAt()).isEqualTo(START.plusSeconds(60));
        assertThat(conversation.unreadCount()).isEqualTo(2);
    }

    @Test
    void thePreviewIsOneShortLine() {
        String body = "  First line\n\n  second\tline " + "x".repeat(300);
        conversation.record(message(Message.Direction.INBOUND, body, START), "Ada", URL);

        assertThat(conversation.preview())
                .startsWith("First line second line x")
                .endsWith("…")
                .hasSize(Message.PREVIEW_LENGTH);
    }

    private static Message message(Message.Direction direction, String body, Instant sentAt) {
        return new Message(
                UUID.randomUUID(), UUID.randomUUID().toString(), direction, Message.Detail.NOTICE, body, sentAt);
    }
}
