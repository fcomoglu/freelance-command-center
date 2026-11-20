package com.freelancecommandcenter.messaging.domain;

import com.freelancecommandcenter.shared.id.ConversationId;
import com.freelancecommandcenter.shared.id.WorkspaceId;
import com.freelancecommandcenter.shared.platform.PlatformCode;
import java.net.URI;
import java.time.Instant;
import java.util.Objects;

/**
 * A conversation in a workspace's inbox, unique per platform conversation ({@code platform} + {@code externalId}). Its
 * summary (other party, link, level of detail, preview) follows the newest message; every new inbound message counts as
 * unread until the conversation is read in the application.
 */
public final class Conversation {

    private final ConversationId id;
    private final WorkspaceId workspaceId;
    private final PlatformCode platform;
    private final String externalId;
    private String counterpart;
    private URI url;
    private Message.Detail detail;
    private Instant lastMessageAt;
    private String preview;
    private int unreadCount;

    private Conversation(
            ConversationId id,
            WorkspaceId workspaceId,
            PlatformCode platform,
            String externalId,
            String counterpart,
            URI url,
            Message.Detail detail,
            Instant lastMessageAt,
            String preview,
            int unreadCount) {
        this.id = Objects.requireNonNull(id, "id");
        this.workspaceId = Objects.requireNonNull(workspaceId, "workspaceId");
        this.platform = Objects.requireNonNull(platform, "platform");
        this.externalId = Objects.requireNonNull(externalId, "externalId");
        this.counterpart = Objects.requireNonNull(counterpart, "counterpart");
        this.url = Objects.requireNonNull(url, "url");
        this.detail = Objects.requireNonNull(detail, "detail");
        this.lastMessageAt = Objects.requireNonNull(lastMessageAt, "lastMessageAt");
        this.preview = Objects.requireNonNull(preview, "preview");
        if (unreadCount < 0) {
            throw new IllegalArgumentException("unreadCount must not be negative");
        }
        this.unreadCount = unreadCount;
    }

    /** An empty conversation; its first message is applied with {@link #record}. */
    public static Conversation start(
            WorkspaceId workspaceId,
            PlatformCode platform,
            String externalId,
            String counterpart,
            URI url,
            Message.Detail detail,
            Instant startedAt) {
        return new Conversation(
                ConversationId.newId(), workspaceId, platform, externalId, counterpart, url, detail, startedAt, "", 0);
    }

    /** Rebuilds a conversation from storage. */
    public static Conversation restore(
            ConversationId id,
            WorkspaceId workspaceId,
            PlatformCode platform,
            String externalId,
            String counterpart,
            URI url,
            Message.Detail detail,
            Instant lastMessageAt,
            String preview,
            int unreadCount) {
        return new Conversation(
                id, workspaceId, platform, externalId, counterpart, url, detail, lastMessageAt, preview, unreadCount);
    }

    /**
     * Applies a message that is new to this conversation. A message older than the newest one, delivered late, counts
     * as unread but leaves the summary alone.
     *
     * @param counterpart the other party's name as the message shows it
     * @param url the conversation's link as the message shows it
     */
    public void record(Message message, String counterpart, URI url) {
        Objects.requireNonNull(message, "message");
        if (message.direction() == Message.Direction.INBOUND) {
            unreadCount++;
        }
        if (message.sentAt().isBefore(lastMessageAt)) {
            return;
        }
        this.counterpart = Objects.requireNonNull(counterpart, "counterpart");
        this.url = Objects.requireNonNull(url, "url");
        this.detail = message.detail();
        this.lastMessageAt = message.sentAt();
        this.preview = message.preview();
    }

    public void markRead() {
        unreadCount = 0;
    }

    public ConversationId id() {
        return id;
    }

    public WorkspaceId workspaceId() {
        return workspaceId;
    }

    public PlatformCode platform() {
        return platform;
    }

    public String externalId() {
        return externalId;
    }

    public String counterpart() {
        return counterpart;
    }

    public URI url() {
        return url;
    }

    /** The level of detail of the newest message. */
    public Message.Detail detail() {
        return detail;
    }

    public Instant lastMessageAt() {
        return lastMessageAt;
    }

    /** The start of the newest message, on one line; empty when it has no text. */
    public String preview() {
        return preview;
    }

    public int unreadCount() {
        return unreadCount;
    }
}
