package com.freelancecommandcenter.platform.api;

import com.freelancecommandcenter.shared.id.WorkspaceId;
import java.util.Objects;

/**
 * Published for every platform notification read for a workspace, from a platform API or a forwarded e-mail. The same
 * notification can be published again, so handlers must be idempotent.
 */
public record NotificationReceived(PlatformNotification notification, WorkspaceId workspaceId) {

    public NotificationReceived {
        Objects.requireNonNull(notification, "notification");
        Objects.requireNonNull(workspaceId, "workspaceId");
    }
}
