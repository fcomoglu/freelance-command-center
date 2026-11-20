package com.freelancecommandcenter.notification.application.port.out;

import com.freelancecommandcenter.notification.domain.Notification;
import com.freelancecommandcenter.shared.id.WorkspaceId;
import com.freelancecommandcenter.shared.pagination.KeysetCursor;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** The notification feeds of the workspaces. */
public interface NotificationRepository {

    /** @return {@code false} when the workspace already has this platform notification */
    boolean add(Notification notification);

    /** Newest first by {@code occurredAt}, then by id, after the cursor position. */
    List<Notification> findRecent(WorkspaceId workspaceId, Optional<KeysetCursor> after, int limit);

    /** @return {@code false} when the workspace has no such notification */
    boolean markRead(WorkspaceId workspaceId, UUID notificationId, Instant at);

    void markAllRead(WorkspaceId workspaceId, Instant at);
}
