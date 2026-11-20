package com.freelancecommandcenter.messaging.api;

import com.freelancecommandcenter.shared.id.ConversationId;
import com.freelancecommandcenter.shared.id.WorkspaceId;
import com.freelancecommandcenter.shared.platform.PlatformCode;
import java.net.URI;
import java.util.UUID;

/**
 * A new message from the other party entered a workspace's inbox. Listeners must tolerate repeats for the same message.
 *
 * @param preview the start of the message on one line; empty when the source showed no text
 * @param url the conversation on its platform
 */
public record MessageArrived(
        WorkspaceId workspaceId,
        ConversationId conversationId,
        UUID messageId,
        PlatformCode platform,
        String counterpart,
        String preview,
        URI url) {}
