package com.freelancecommandcenter.platform.api;

import com.freelancecommandcenter.shared.id.WorkspaceId;
import java.util.Objects;

/**
 * Published for every message read for a workspace, from a platform API or a forwarded e-mail. The same message can be
 * published again, so handlers must be idempotent.
 */
public record MessageReceived(PlatformMessage message, WorkspaceId workspaceId) {

    public MessageReceived {
        Objects.requireNonNull(message, "message");
        Objects.requireNonNull(workspaceId, "workspaceId");
    }
}
